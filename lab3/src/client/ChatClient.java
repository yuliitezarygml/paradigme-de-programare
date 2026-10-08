package client;

import common.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.List;

/**
 * Сетевой клиент, отвечающий за TCP socket-соединение с сервером.
 * Асинхронно принимает сообщения и предоставляет методы для:
 * - отправки обычных текстовых сообщений
 * - отправки ответов (Reply / цитирование)
 * - передачи файлов через сеть
 * - навигации и создания новых комнат чата
 */
public class ChatClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username = "Аноним";
    private String currentRoom = "#general";
    private volatile boolean connected = false;
    private ClientListener listener;

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    /**
     * Инициирует подключение к серверу в отдельном фоновом потоке,
     * чтобы не блокировать графический поток интерфейса (Swing EDT).
     */
    public void connect(String host, int port, String requestedUsername) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                connected = true;

                // Отправка первичного запроса на подключение с именем пользователя
                NetworkMessage connMsg = new NetworkMessage(MessageType.CONNECT, requestedUsername, "#general");
                connMsg.setText(requestedUsername);
                sendMessageDirect(connMsg);

                // Запуск отдельного потока для постоянного прослушивания входящих пакетов
                new Thread(this::receiveLoop, "ChatClient-Receiver").start();

            } catch (Exception e) {
                connected = false;
                if (listener != null) {
                    listener.onConnectionFailed("Не удалось подключиться к " + host + ":" + port + " (" + e.getMessage() + ")");
                }
            }
        }, "ChatClient-ConnectThread").start();
    }

    /**
     * Непрерывный цикл чтения входящих сообщений от сервера.
     */
    private void receiveLoop() {
        try {
            while (connected && !socket.isClosed()) {
                Object obj = in.readObject();
                if (!(obj instanceof NetworkMessage)) continue;

                NetworkMessage msg = (NetworkMessage) obj;
                handleServerMessage(msg);
            }
        } catch (EOFException | SocketException e) {
            // Соединение разорвано со стороны сервера или сети
        } catch (Exception e) {
            if (connected) {
                System.err.println("[ChatClient] Ошибка при чтении потока данных: " + e.getMessage());
            }
        } finally {
            disconnect("Соединение с сервером было потеряно.");
        }
    }

    /**
     * Обработка принятого сетевого пакета от сервера.
     */
    private void handleServerMessage(NetworkMessage msg) {
        if (msg == null || listener == null) return;

        switch (msg.getType()) {
            case CONNECT_ACK:
                this.username = msg.getText();
                this.currentRoom = msg.getTargetRoom() != null ? msg.getTargetRoom() : "#general";
                listener.onConnected(this.username, msg.getRooms(), msg.getRoomUsers(), msg.getHistory());
                break;

            case CHAT_MESSAGE:
                listener.onMessageReceived(msg);
                break;

            case FILE_TRANSFER:
                listener.onFileReceived(msg);
                break;

            case ROOM_LIST:
                listener.onRoomListUpdated(msg.getRooms());
                break;

            case USER_LIST:
                listener.onUserListUpdated(msg.getRoomUsers());
                break;

            case HISTORY_RESPONSE:
                if (msg.getTargetRoom() != null) {
                    this.currentRoom = msg.getTargetRoom();
                }
                listener.onRoomHistoryLoaded(this.currentRoom, msg.getHistory());
                if (msg.getRoomUsers() != null) {
                    listener.onUserListUpdated(msg.getRoomUsers());
                }
                if (msg.getRooms() != null) {
                    listener.onRoomListUpdated(msg.getRooms());
                }
                break;

            case SERVER_NOTIFICATION:
                listener.onNotification(msg.getText());
                break;

            case ERROR:
                listener.onError(msg.getErrorMessage());
                break;

            case KICK:
                disconnect("Вы были отключены от сервера: " + msg.getText());
                break;

            default:
                break;
        }
    }

    /**
     * Отправка текстового сообщения или ответа с цитированием (Reply).
     */
    public boolean sendChatMessage(String text, String replyToId, String replyToAuthor, String replyToSnippet) {
        if (!connected) return false;
        NetworkMessage msg;
        if (replyToAuthor != null && !replyToAuthor.isEmpty()) {
            msg = NetworkMessage.createReplyMessage(username, currentRoom, text, replyToId, replyToAuthor, replyToSnippet);
        } else {
            msg = NetworkMessage.createTextMessage(username, currentRoom, text);
        }
        return sendMessageDirect(msg);
    }

    /**
     * Передача бинарного файла в текущую комнату чата.
     */
    public boolean sendFile(File file, String replyToId, String replyToAuthor, String replyToSnippet) {
        if (!connected || file == null || !file.exists()) return false;
        try {
            long size = file.length();
            // Проверка разумного лимита размера файла в ОЗУ (до 50 МБ)
            if (size > 50 * 1024 * 1024) {
                if (listener != null) {
                    listener.onError("Размер файла превышает рекомендуемый лимит в 50 МБ!");
                }
                return false;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            NetworkMessage msg = NetworkMessage.createFileMessage(
                    username, currentRoom, file.getName(), size, bytes,
                    replyToId, replyToAuthor, replyToSnippet
            );
            return sendMessageDirect(msg);
        } catch (Exception e) {
            if (listener != null) {
                listener.onError("Ошибка при чтении файла: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Запрос на переключение в другую комнату чата.
     */
    public void joinRoom(String roomName) {
        if (!connected || roomName == null) return;
        this.currentRoom = roomName;
        NetworkMessage msg = new NetworkMessage(MessageType.JOIN_ROOM, username, roomName);
        sendMessageDirect(msg);
    }

    /**
     * Запрос на создание новой комнаты чата.
     */
    public void createRoom(String roomName, String description) {
        if (!connected || roomName == null) return;
        NetworkMessage msg = new NetworkMessage(MessageType.CREATE_ROOM, username, currentRoom);
        msg.setText(roomName);
        msg.setReplyToSnippet(description);
        sendMessageDirect(msg);
    }

    /**
     * Запрос истории сообщений для комнаты.
     */
    public void requestHistory(String roomName) {
        if (!connected) return;
        NetworkMessage msg = new NetworkMessage(MessageType.HISTORY_REQUEST, username, roomName);
        sendMessageDirect(msg);
    }

    /**
     * Потокобезопасная прямая отправка пакета в сокет.
     */
    private synchronized boolean sendMessageDirect(NetworkMessage msg) {
        if (!connected || out == null || socket.isClosed()) return false;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
            return true;
        } catch (IOException e) {
            disconnect("Ошибка при передаче данных на сервер.");
            return false;
        }
    }

    /**
     * Корректное отключение от сервера с освобождением ресурсов.
     */
    public synchronized void disconnect(String reason) {
        if (!connected) return;
        connected = false;

        try {
            if (out != null) {
                NetworkMessage discMsg = new NetworkMessage(MessageType.DISCONNECT, username, currentRoom);
                out.writeObject(discMsg);
                out.flush();
            }
        } catch (Exception ignored) {}

        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}

        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }
}
