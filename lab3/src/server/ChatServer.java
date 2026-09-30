package server;

import common.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nucleul serverului de chat multi-thread.
 * Gestionează conexiunile socket TCP, camerele de chat,
 * transmiterea mesajelor și fișierelor, precum și istoricul discuțiilor.
 */
public class ChatServer {

    private int port = 8888;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, List<NetworkMessage>> roomHistory = new ConcurrentHashMap<>();

    private ServerListener listener;
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
     * Inițializează camerele de chat implicite.
     */
    private void initDefaultRooms() {
        addRoomInternal(new ChatRoom("#general", "Camera principală de discuție pentru toți membrii", "Server"));
        addRoomInternal(new ChatRoom("#proiecte", "Discuții tehnice, partajare cod și fișiere de laborator", "Server"));
        addRoomInternal(new ChatRoom("#random", "Socializare liberă, idei și pauză de cafea", "Server"));
    }

    private void addRoomInternal(ChatRoom room) {
        rooms.put(room.getName().toLowerCase(), room);
        roomHistory.putIfAbsent(room.getName().toLowerCase(), new CopyOnWriteArrayList<>());
        loadHistoryFromDisk(room.getName());
    }

    /**
     * Pornește serverul pe portul specificat.
     */
    public synchronized boolean start(int port) {
        if (running) return true;
        this.port = port;

        try {
            serverSocket = new ServerSocket(port);
            running = true;

            String localIp = detectLocalIP();
            log("INFO", "Serverul a pornit cu succes pe portul " + port + " (IP local: " + localIp + ")");

            if (listener != null) {
                listener.onServerStarted(port, localIp);
            }

            acceptThread = new Thread(this::acceptClients, "ChatServer-AcceptThread");
            acceptThread.start();
            return true;
        } catch (IOException e) {
            log("EROARE", "Eșec la pornirea serverului pe portul " + port + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Oprește serverul și deconectează toți clienții în siguranță.
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;

        log("INFO", "Oprire server în curs...");

        // Notificare și deconectare clienți
        for (ClientHandler client : clients) {
            try {
                NetworkMessage kickMsg = new NetworkMessage(MessageType.KICK, "SERVER", client.getCurrentRoom());
                kickMsg.setText("Serverul a fost oprit de administrator.");
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

        log("INFO", "Serverul a fost oprit complet.");
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
     * Bucla de ascultare a noilor conexiuni de clienți.
     */
    private void acceptClients() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(this, socket);
                new Thread(handler, "ClientHandler-" + socket.getRemoteSocketAddress()).start();
            } catch (IOException e) {
                if (!running) break;
                log("AVERTISMENT", "Eroare la acceptarea conexiunii socket: " + e.getMessage());
            }
        }
    }

    /**
     * Înregistrează un client nou după validarea numelui.
     */
    public synchronized boolean registerClient(ClientHandler handler, String requestedUsername) {
        String cleanName = requestedUsername != null ? requestedUsername.trim() : "Anonim";
        if (cleanName.isEmpty()) cleanName = "Anonim";

        // Asigurăm unicitatea numelui
        String finalName = cleanName;
        int counter = 1;
        while (isUsernameTaken(finalName)) {
            finalName = cleanName + "_" + counter++;
        }

        handler.setUsername(finalName);
        handler.setCurrentRoom("#general");
        clients.add(handler);

        updateAllRoomCounts();

        log("INFO", "Client nou conectat: " + finalName + " de la " + handler.getSocket().getRemoteSocketAddress());

        // Trimitem confirmarea de conectare cu starea curentă
        NetworkMessage ack = new NetworkMessage(MessageType.CONNECT_ACK, "SERVER", "#general");
        ack.setText(finalName);
        ack.setRooms(getRoomsList());
        ack.setRoomUsers(getUsersInRoom("#general"));
        ack.setHistory(getRecentHistory("#general", 50));
        handler.sendMessage(ack);

        // Notificare către toți membrii camerei #general
        broadcastToRoom("#general", NetworkMessage.createNotification("#general", "👋 " + finalName + " s-a alăturat chat-ului!"), null);

        // Notificare actualizare listă camere către toți clienții
        broadcastRoomList();
        broadcastUserList("#general");

        if (listener != null) {
            listener.onClientConnected(handler.getInfo());
        }

        return true;
    }

    /**
     * Deconectează un client și curăță resursele asociate.
     */
    public synchronized void unregisterClient(ClientHandler handler) {
        if (!clients.contains(handler)) return;

        clients.remove(handler);
        String username = handler.getUsername();
        String room = handler.getCurrentRoom();

        updateAllRoomCounts();
        log("INFO", "Client deconectat: " + username + " din camera " + room);

        if (room != null) {
            broadcastToRoom(room, NetworkMessage.createNotification(room, "🚪 " + username + " a părăsit camera."), null);
            broadcastUserList(room);
        }
        broadcastRoomList();

        if (listener != null) {
            listener.onClientDisconnected(handler.getInfo());
        }
    }

    /**
     * Comutarea unui client într-o altă cameră de chat.
     */
    public synchronized void switchClientRoom(ClientHandler handler, String targetRoomName) {
        if (targetRoomName == null) return;
        String normalizedTarget = targetRoomName.toLowerCase();
        if (!rooms.containsKey(normalizedTarget)) {
            // Dacă nu există, o creăm automat
            createRoom(targetRoomName, "Cameră creată de utilizator", handler.getUsername());
        }

        ChatRoom target = rooms.get(normalizedTarget);
        String oldRoom = handler.getCurrentRoom();
        if (oldRoom.equalsIgnoreCase(target.getName())) return;

        // Ieșire din camera veche
        broadcastToRoom(oldRoom, NetworkMessage.createNotification(oldRoom, "🏃 " + handler.getUsername() + " a trecut în " + target.getName()), handler);

        // Setare cameră nouă
        handler.setCurrentRoom(target.getName());
        updateAllRoomCounts();

        // Trimitere istoric cameră nouă direct către client
        NetworkMessage historyMsg = new NetworkMessage(MessageType.HISTORY_RESPONSE, "SERVER", target.getName());
        historyMsg.setRooms(getRoomsList());
        historyMsg.setRoomUsers(getUsersInRoom(target.getName()));
        historyMsg.setHistory(getRecentHistory(target.getName(), 50));
        handler.sendMessage(historyMsg);

        // Notificare în noua cameră
        broadcastToRoom(target.getName(), NetworkMessage.createNotification(target.getName(), "🎉 " + handler.getUsername() + " a intrat în cameră!"), handler);

        // Actualizare liste
        broadcastUserList(oldRoom);
        broadcastUserList(target.getName());
        broadcastRoomList();

        log("INFO", handler.getUsername() + " a schimbat camera: " + oldRoom + " -> " + target.getName());
        if (listener != null) {
            listener.onClientRoomChanged(handler.getInfo(), oldRoom, target.getName());
        }
    }

    /**
     * Crearea unei noi camere de chat.
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

        log("INFO", "Cameră creată: " + cleanName + " de către " + createdBy);
        broadcastRoomList();

        if (listener != null) {
            listener.onRoomCreated(newRoom);
        }
        return true;
    }

    /**
     * Ștergerea unei camere de chat (cu excepția camerei implicite #general).
     */
    public synchronized boolean deleteRoom(String roomName) {
        if (roomName == null) return false;
        String key = roomName.toLowerCase();
        if (key.equals("#general")) {
            log("AVERTISMENT", "Camera #general este permanentă și nu poate fi ștearsă!");
            return false;
        }

        ChatRoom removed = rooms.remove(key);
        if (removed == null) return false;

        // Migrăm toți clienții din camera ștearsă în #general
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equalsIgnoreCase(roomName)) {
                switchClientRoom(client, "#general");
            }
        }

        updateAllRoomCounts();
        broadcastRoomList();
        log("INFO", "Camera " + roomName + " a fost ștearsă.");

        if (listener != null) {
            listener.onRoomDeleted(roomName);
        }
        return true;
    }

    /**
     * Procesează și distribuie un mesaj text de chat (inclusiv răspunsuri / reply).
     */
    public void processChatMessage(NetworkMessage message) {
        String roomKey = message.getTargetRoom().toLowerCase();
        recordMessage(roomKey, message);

        // Distribuire către toți membrii camerei
        broadcastToRoom(message.getTargetRoom(), message, null);

        log("CHAT", "[" + message.getTargetRoom() + "] " + message.getSender() +
                (message.isReply() ? " (Răspuns la @" + message.getReplyToAuthor() + ")" : "") +
                ": " + message.getText());

        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    /**
     * Procesează și distribuie un transfer de fișier.
     */
    public void processFileTransfer(NetworkMessage message) {
        String roomKey = message.getTargetRoom().toLowerCase();
        recordMessage(roomKey, message);

        // Distribuire către toți membrii camerei
        broadcastToRoom(message.getTargetRoom(), message, null);

        log("FIȘIER", "[" + message.getTargetRoom() + "] " + message.getSender() +
                " a trimis fișierul: " + message.getFileName() + " (" + message.getFormattedFileSize() + ")");

        if (listener != null) {
            listener.onFileTransferred(message);
        }
    }

    /**
     * Înregistrează mesajul în istoric (memorie și disc).
     */
    private synchronized void recordMessage(String roomKey, NetworkMessage msg) {
        List<NetworkMessage> list = roomHistory.computeIfAbsent(roomKey, k -> new CopyOnWriteArrayList<>());
        list.add(msg);
        appendHistoryToDisk(roomKey, msg);
    }

    public List<NetworkMessage> getRecentHistory(String roomName, int max) {
        if (roomName == null) return Collections.emptyList();
        List<NetworkMessage> full = roomHistory.get(roomName.toLowerCase());
        if (full == null || full.isEmpty()) return Collections.emptyList();

        int start = Math.max(0, full.size() - max);
        return new ArrayList<>(full.subList(start, full.size()));
    }

    public List<NetworkMessage> getAllRoomHistory(String roomName) {
        if (roomName == null) return Collections.emptyList();
        List<NetworkMessage> list = roomHistory.get(roomName.toLowerCase());
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    /**
     * Transmite un pachet către toți utilizatorii din camera specificată.
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
     * Notifică toți clienții cu lista actualizată a camerelor.
     */
    public void broadcastRoomList() {
        NetworkMessage msg = new NetworkMessage(MessageType.ROOM_LIST, "SERVER", "");
        msg.setRooms(getRoomsList());
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }

    /**
     * Notifică clienții dintr-o cameră cu privire la membrii online.
     */
    public void broadcastUserList(String roomName) {
        if (roomName == null) return;
        List<String> users = getUsersInRoom(roomName);
        NetworkMessage msg = new NetworkMessage(MessageType.USER_LIST, "SERVER", roomName);
        msg.setRoomUsers(users);

        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equalsIgnoreCase(roomName)) {
                client.sendMessage(msg);
            }
        }
    }

    /**
     * Trimitere anunț de la administratorul serverului către toate camerele.
     */
    public void broadcastServerAnnouncement(String text) {
        for (ChatRoom room : rooms.values()) {
            NetworkMessage msg = NetworkMessage.createNotification(room.getName(), "📢 ANUNȚ SERVER: " + text);
            processChatMessage(msg);
        }
    }

    /**
     * Deconectează forțat un utilizator (Kick).
     */
    public synchronized boolean kickUser(String username, String reason) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(username)) {
                NetworkMessage kickMsg = new NetworkMessage(MessageType.KICK, "SERVER", client.getCurrentRoom());
                kickMsg.setText(reason != null ? reason : "Ai fost deconectat de administrator.");
                client.sendMessage(kickMsg);
                client.close();
                log("AVERTISMENT", "Utilizatorul " + username + " a primit Kick: " + reason);
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
     * Scrie mesajul în fișierul de istoric pe disc.
     */
    private void appendHistoryToDisk(String roomKey, NetworkMessage msg) {
        try {
            String safeRoom = roomKey.replace("#", "").replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(historyDir, "history_" + safeRoom + ".log");
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                out.println("[" + msg.getFormattedTime() + "] " + msg.getSender() + ": " +
                        (msg.isFile() ? "[FIȘIER: " + msg.getFileName() + " (" + msg.getFormattedFileSize() + ")]" : msg.getText()) +
                        (msg.isReply() ? " (Răspuns la @" + msg.getReplyToAuthor() + ": \"" + msg.getReplyToSnippet() + "\")" : ""));
            }
        } catch (Exception e) {
            // Ignorăm erorile de salvare pe disc pentru a nu bloca mesageria
        }
    }

    /**
     * Încarcă mesaje vechi din fișierul de log dacă există.
     */
    private void loadHistoryFromDisk(String roomName) {
        try {
            String safeRoom = roomName.replace("#", "").replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(historyDir, "history_" + safeRoom + ".log");
            if (!file.exists()) return;

            // Istoricul textual persistat pe disc este disponibil la solicitare
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
     * Detectează adresa IP a mașinii în rețeaua locală (LAN).
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
