package server;

import common.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Многопоточное ядро чат-сервера.
 * Управляет входящими TCP socket соединениями, комнатами чата,
 * маршрутизацией текстовых сообщений и файлов, а также персистентной историей переписки.
 */
public class ChatServer {

    // Порт по умолчанию для прослушивания входящих подключений
    private int port = 8888;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;

    // Потокобезопасный список подключенных обработчиков клиентов
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    // Карта зарегистрированных комнат чата (ключ: имя комнаты в нижнем регистре)
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    // История сообщений для каждой комнаты
    private final Map<String, List<NetworkMessage>> roomHistory = new ConcurrentHashMap<>();

    // Слушатель событий сервера (для передачи данных в GUI)
    private ServerListener listener;
    // Директория для хранения логов истории на диске
    private final File historyDir;

    public ChatServer() {
        File dir = new File("history");
        if (!dir.exists() && new File("lab3").exists()) {
            dir = new File("lab3/history");
        }
        this.historyDir = dir;
        if (!this.historyDir.exists()) {
            this.historyDir.mkdirs();
        }
        initDefaultRooms();
    }

    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    /**
     * Инициализирует стандартные комнаты чата по умолчанию.
     */
    private void initDefaultRooms() {
        addRoomInternal(new ChatRoom("#general", "Основная комната общения для всех участников", "Server"));
        addRoomInternal(new ChatRoom("#proiecte", "Технические обсуждения, обмен кодом и файлами лабораторных", "Server"));
        addRoomInternal(new ChatRoom("#random", "Свободное общение, идеи и отдых", "Server"));
    }

    private void addRoomInternal(ChatRoom room) {
        rooms.put(room.getName().toLowerCase(), room);
        roomHistory.putIfAbsent(room.getName().toLowerCase(), new CopyOnWriteArrayList<>());
        loadHistoryFromDisk(room.getName());
    }

    /**
     * Запускает сервер на указанном порту.
     */
    public synchronized boolean start(int port) {
        if (running) return true;
        this.port = port;

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            String localIp = detectLocalIP();
            log("INFO", "Сервер успешно запущен на порту " + port + " (Локальный IP: " + localIp + ")");

            if (listener != null) {
                listener.onServerStarted(port, localIp);
            }

            // Фоновый поток для приема новых входящих подключений клиентов
            acceptThread = new Thread(this::acceptClients, "ChatServer-AcceptThread");
            acceptThread.start();
            return true;
        } catch (IOException e) {
            log("ОШИБКА", "Не удалось запустить сервер на порту " + port + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Корректно останавливает сервер и безопасно отключает всех активных клиентов.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;

        log("INFO", "Идет остановка сервера...");

        // Оповещение и принудительное отключение всех клиентов
        for (ClientHandler client : clients) {
            try {
                NetworkMessage kickMsg = new NetworkMessage(MessageType.KICK, "СЕРВЕР", client.getCurrentRoom());
                kickMsg.setText("Сервер был остановлен администратором.");
                client.sendMessage(kickMsg);
                client.close();
            } catch (Exception ignored) {}
        }
        clients.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        updateAllRoomCounts();

        log("INFO", "Сервер полностью остановлен.");
        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public List<ClientInfo> getConnectedClientsInfo() {
        List<ClientInfo> list = new ArrayList<>();
        for (ClientHandler ch : clients) {
            list.add(ch.getInfo());
        }
        return list;
    }

    public List<ChatRoom> getRoomsList() {
        return new ArrayList<>(rooms.values());
    }

    /**
     * Цикл ожидания и принятия новых клиентских socket-соединений.
     */
    private void acceptClients() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(this, socket);
                new Thread(handler, "ClientHandler-" + socket.getRemoteSocketAddress()).start();
            } catch (IOException e) {
                if (!running) break;
                log("ПРЕДУПРЕЖДЕНИЕ", "Ошибка при принятии socket-соединения: " + e.getMessage());
            }
        }
    }

    /**
     * Регистрирует нового клиента после проверки уникальности имени пользователя.
     */
    public synchronized boolean registerClient(ClientHandler handler, String requestedUsername) {
        String cleanName = requestedUsername != null ? requestedUsername.trim() : "Аноним";
        if (cleanName.isEmpty()) cleanName = "Аноним";

        // Обеспечиваем уникальность никнейма (добавляем суффикс _1, _2 при коллизии)
        String finalName = cleanName;
        int counter = 1;
        while (isUsernameTaken(finalName)) {
            finalName = cleanName + "_" + counter++;
        }

        handler.setUsername(finalName);
        handler.setCurrentRoom("#general");
        clients.add(handler);

        updateAllRoomCounts();

        log("INFO", "Подключен новый клиент: " + finalName + " с адреса " + handler.getSocket().getRemoteSocketAddress());

        // Отправляем клиенту подтверждение подключения с начальным состоянием
        NetworkMessage ack = new NetworkMessage(MessageType.CONNECT_ACK, "СЕРВЕР", "#general");
        ack.setText(finalName);
        ack.setRooms(getRoomsList());
        ack.setRoomUsers(getUsersInRoom("#general"));
        ack.setHistory(getRecentHistory("#general", 50));
        handler.sendMessage(ack);

        // Системное уведомление всем участникам комнаты #general
        broadcastToRoom("#general", NetworkMessage.createNotification("#general", "👋 " + finalName + " присоединился к чату!"), null);

        // Рассылаем обновленный список комнат и пользователей
        broadcastRoomList();
        broadcastUserList("#general");

        if (listener != null) {
            listener.onClientConnected(handler.getInfo());
        }

        return true;
    }

    /**
     * Отключает клиента и очищает связанные с ним ресурсы.
     */
    public synchronized void unregisterClient(ClientHandler handler) {
        if (!clients.contains(handler)) return;

        clients.remove(handler);
        String username = handler.getUsername();
        String room = handler.getCurrentRoom();

        updateAllRoomCounts();
        log("INFO", "Клиент отключился: " + username + " из комнаты " + room);

        if (room != null) {
            broadcastToRoom(room, NetworkMessage.createNotification(room, "🚪 " + username + " покинул чат."), null);
            broadcastUserList(room);
        }
        broadcastRoomList();

        if (listener != null) {
            listener.onClientDisconnected(handler.getInfo());
        }
    }

    /**
     * Переключение клиента в другую комнату чата.
     */
    public synchronized void switchClientRoom(ClientHandler handler, String targetRoomName) {
        if (targetRoomName == null) return;
        String normalizedTarget = targetRoomName.toLowerCase();
        if (!rooms.containsKey(normalizedTarget)) {
            // Если комната не найдена, создаем ее автоматически
            createRoom(targetRoomName, "Комната создана пользователем", handler.getUsername());
        }

        ChatRoom target = rooms.get(normalizedTarget);
        String oldRoom = handler.getCurrentRoom();
        if (oldRoom.equalsIgnoreCase(target.getName())) return;

        // Уведомление в старую комнату о выходе пользователя
        broadcastToRoom(oldRoom, NetworkMessage.createNotification(oldRoom, "🏃 " + handler.getUsername() + " перешел в " + target.getName()), handler);

        // Установка новой комнаты для клиента
        handler.setCurrentRoom(target.getName());
        updateAllRoomCounts();

        // Отправка истории сообщений новой комнаты напрямую клиенту
        NetworkMessage historyMsg = new NetworkMessage(MessageType.HISTORY_RESPONSE, "СЕРВЕР", target.getName());
        historyMsg.setRooms(getRoomsList());
        historyMsg.setRoomUsers(getUsersInRoom(target.getName()));
        historyMsg.setHistory(getRecentHistory(target.getName(), 50));
        handler.sendMessage(historyMsg);

        // Приветственное системное уведомление в новой комнате
        broadcastToRoom(target.getName(), NetworkMessage.createNotification(target.getName(), "🎉 " + handler.getUsername() + " вошел в комнату!"), handler);

        // Обновление списков для всех участников
        broadcastUserList(oldRoom);
        broadcastUserList(target.getName());
        broadcastRoomList();

        log("INFO", handler.getUsername() + " сменил комнату: " + oldRoom + " -> " + target.getName());
        if (listener != null) {
            listener.onClientRoomChanged(handler.getInfo(), oldRoom, target.getName());
        }
    }

    /**
     * Создание новой комнаты чата.
     */
    public synchronized boolean createRoom(String roomName, String description, String createdBy) {
        if (roomName == null || roomName.trim().isEmpty()) return false;
        String cleanName = roomName.trim();
        if (!cleanName.startsWith("#")) cleanName = "#" + cleanName;

        String key = cleanName.toLowerCase();
        if (rooms.containsKey(key)) {
            return false;
        }

        ChatRoom newRoom = new ChatRoom(cleanName, description, createdBy);
        addRoomInternal(newRoom);
        updateAllRoomCounts();

        log("INFO", "Комната создана: " + cleanName + " пользователем " + createdBy);
        broadcastRoomList();

        if (listener != null) {
            listener.onRoomCreated(newRoom);
        }
        return true;
    }

    /**
     * Удаление комнаты чата (удаление базовой комнаты #general запрещено).
     */
    public synchronized boolean deleteRoom(String roomName) {
        if (roomName == null) return false;
        String key = roomName.toLowerCase();
        if (key.equals("#general")) {
            log("ПРЕДУПРЕЖДЕНИЕ", "Главная комната #general является постоянной и не может быть удалена!");
            return false;
        }

        ChatRoom removed = rooms.remove(key);
        if (removed == null) return false;

        // Автоматически и безопасно переносим всех участников удаленной комнаты в #general
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equalsIgnoreCase(roomName)) {
                switchClientRoom(client, "#general");
            }
        }

        updateAllRoomCounts();
        broadcastRoomList();
        log("INFO", "Комната " + roomName + " была удалена.");

        if (listener != null) {
            listener.onRoomDeleted(roomName);
        }
        return true;
    }

    /**
     * Обрабатывает и рассылает текстовое сообщение (включая сообщения-ответы / Reply).
     */
    public void processChatMessage(NetworkMessage message) {
        String roomKey = message.getTargetRoom().toLowerCase();
        recordMessage(roomKey, message);

        // Рассылаем всем участникам комнаты
        broadcastToRoom(message.getTargetRoom(), message, null);

        log("ЧАТ", "[" + message.getTargetRoom() + "] " + message.getSender() +
                (message.isReply() ? " (Ответ для @" + message.getReplyToAuthor() + ")" : "") +
                ": " + message.getText());

        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    /**
     * Обрабатывает и рассылает переданный файл.
     */
    public void processFileTransfer(NetworkMessage message) {
        String roomKey = message.getTargetRoom().toLowerCase();
        recordMessage(roomKey, message);

        // Рассылаем файл всем участникам комнаты
        broadcastToRoom(message.getTargetRoom(), message, null);

        log("ФАЙЛ", "[" + message.getTargetRoom() + "] " + message.getSender() +
                " отправил файл: " + message.getFileName() + " (" + message.getFormattedFileSize() + ")");

        if (listener != null) {
            listener.onFileTransferred(message);
        }
    }

    /**
     * Сохраняет сообщение в оперативной памяти и записывает в файл журнала на диске.
     */
    private synchronized void recordMessage(String roomKey, NetworkMessage msg) {
        List<NetworkMessage> list = roomHistory.computeIfAbsent(roomKey, k -> new CopyOnWriteArrayList<>());
        list.add(msg);
        appendHistoryToDisk(roomKey, msg);
    }

    /**
     * Возвращает последние max сообщений из истории комнаты.
     */
    public List<NetworkMessage> getRecentHistory(String roomName, int max) {
        if (roomName == null) return Collections.emptyList();
        List<NetworkMessage> full = roomHistory.get(roomName.toLowerCase());
        if (full == null || full.isEmpty()) return Collections.emptyList();

        int start = Math.max(0, full.size() - max);
        return new ArrayList<>(full.subList(start, full.size()));
    }

    /**
     * Возвращает полную историю сообщений комнаты.
     */
    public List<NetworkMessage> getAllRoomHistory(String roomName) {
        if (roomName == null) return Collections.emptyList();
        List<NetworkMessage> list = roomHistory.get(roomName.toLowerCase());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    /**
     * Передает пакет всем клиентам, находящимся в указанной комнате.
     */
    public void broadcastToRoom(String roomName, NetworkMessage msg, ClientHandler except) {
        if (roomName == null) return;
        for (ClientHandler client : clients) {
            if (client != except && client.getCurrentRoom().equalsIgnoreCase(roomName)) {
                client.sendMessage(msg);
            }
        }
    }

    /**
     * Рассылает всем клиентам обновленный список комнат.
     */
    public void broadcastRoomList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ROOM_LIST, "СЕРВЕР", "");
        msg.setRooms(getRoomsList());
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

    /**
     * Рассылает список активных участников для указанной комнаты.
     */
    public void broadcastUserList(String roomName) {
        if (roomName == null) return;
        List<String> users = getUsersInRoom(roomName);
        NetworkMessage msg = new NetworkMessage(MessageType.USER_LIST, "СЕРВЕР", roomName);
        msg.setRoomUsers(users);

        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equalsIgnoreCase(roomName)) {
                client.sendMessage(msg);
            }
        }
    }

    /**
     * Отправка глобального объявления администратора сервера во все комнаты одновременно.
     */
    public void broadcastServerAnnouncement(String text) {
        for (ChatRoom room : rooms.values()) {
            NetworkMessage msg = NetworkMessage.createNotification(room.getName(), "📢 ОБЪЯВЛЕНИЕ СЕРВЕРА: " + text);
            processChatMessage(msg);
        }
    }

    /**
     * Принудительное отключение пользователя администратором (Kick).
     */
    public synchronized boolean kickUser(String username, String reason) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(username)) {
                NetworkMessage kickMsg = new NetworkMessage(MessageType.KICK, "СЕРВЕР", client.getCurrentRoom());
                kickMsg.setText(reason != null ? reason : "Вы были отключены администратором.");
                client.sendMessage(kickMsg);
                client.close();
                log("ПРЕДУПРЕЖДЕНИЕ", "Пользователь " + username + " был отключен (Kick): " + reason);
                return true;
            }
        }
        return false;
    }

    public List<String> getUsersInRoom(String roomName) {
        List<String> list = new ArrayList<>();
        if (roomName == null) return list;
        for (ClientHandler ch : clients) {
            if (ch.getCurrentRoom().equalsIgnoreCase(roomName)) {
                list.add(ch.getUsername());
            }
        }
        return list;
    }

    private void updateAllRoomCounts() {
        for (ChatRoom r : rooms.values()) {
            int count = 0;
            for (ClientHandler ch : clients) {
                if (ch.getCurrentRoom().equalsIgnoreCase(r.getName())) {
                    count++;
                }
            }
            r.setUserCount(count);
        }
    }

    private boolean isUsernameTaken(String username) {
        for (ClientHandler ch : clients) {
            if (ch.getUsername().equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    /**
     * Записывает сообщение в файл истории на диске в кодировке UTF-8.
     */
    private void appendHistoryToDisk(String roomKey, NetworkMessage msg) {
        try {
            String safeRoom = roomKey.replace("#", "").replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(historyDir, "history_" + safeRoom + ".log");
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                out.println("[" + msg.getFormattedTime() + "] " + msg.getSender() + ": " +
                        (msg.isFile() ? "[ФАЙЛ: " + msg.getFileName() + " (" + msg.getFormattedFileSize() + ")]" : msg.getText()) +
                        (msg.isReply() ? " (Ответ на @" + msg.getReplyToAuthor() + ": \"" + msg.getReplyToSnippet() + "\")" : ""));
            }
        } catch (Exception e) {
            // Ошибки записи логов не должны блокировать отправку сообщений в сети
        }
    }

    /**
     * Чтение истории сообщений из файла при старте, если он существует.
     */
    private void loadHistoryFromDisk(String roomName) {
        try {
            String safeRoom = roomName.replace("#", "").replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(historyDir, "history_" + safeRoom + ".log");
            if (!file.exists()) return;
        } catch (Exception ignored) {}
    }

    private void log(String level, String message) {
        String logEntry = "[" + level + "] " + message;
        System.out.println(logEntry);
        if (listener != null) {
            listener.onLogEvent(level, message);
        }
    }

    /**
     * Автоматически определяет реальный локальный IP-адрес машины в сети LAN/Wi-Fi.
     */
    public static String detectLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
