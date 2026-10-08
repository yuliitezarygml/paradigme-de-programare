package server;

import common.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Обработчик индивидуального клиентского TCP socket-соединения.
 * Выполняется в отдельном потоке (Thread) для неблокирующего асинхронного приема пакетов.
 */
public class ClientHandler implements Runnable {

    // Ссылка на центральный экземпляр сервера
    private final ChatServer server;
    // Клиентский сокет
    private final Socket socket;
    // Потоки ввода-вывода объектов Java Serialization
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Имя текущего пользователя
    private String username = "Аноним";
    // Текущая комната чата (по умолчанию #general)
    private String currentRoom = "#general";
    // Флаг активного соединения
    private volatile boolean connected = true;
    // Метаданные клиента
    private ClientInfo info;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Инициализация потоков ввода-вывода (ObjectOutputStream перед ObjectInputStream согласно спецификации Java)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Основной цикл чтения входящих сетевых пакетов
            while (connected && !socket.isClosed()) {
                Object obj = in.readObject();
                if (!(obj instanceof NetworkMessage)) continue;

                NetworkMessage msg = (NetworkMessage) obj;
                handleMessage(msg);
            }
        } catch (EOFException | SocketException e) {
            // Клиент закрыл соединение или произошел разрыв связи
        } catch (Exception e) {
            System.err.println("[ClientHandler] Ошибка соединения для " + username + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Диспетчеризация и обработка принятого сетевого пакета по его типу.
     */
    private void handleMessage(NetworkMessage msg) {
        if (msg == null) return;

        switch (msg.getType()) {
            case CONNECT:
                String reqUser = msg.getText();
                server.registerClient(this, reqUser);
                break;

            case CHAT_MESSAGE:
                msg.setSender(this.username);
                msg.setTargetRoom(this.currentRoom);
                server.processChatMessage(msg);
                break;

            case FILE_TRANSFER:
                msg.setSender(this.username);
                msg.setTargetRoom(this.currentRoom);
                server.processFileTransfer(msg);
                break;

            case JOIN_ROOM:
                server.switchClientRoom(this, msg.getTargetRoom());
                break;

            case CREATE_ROOM:
                String roomName = msg.getText();
                String desc = msg.getReplyToSnippet();
                boolean created = server.createRoom(roomName, desc, this.username);
                if (created) {
                    server.switchClientRoom(this, roomName);
                } else {
                    NetworkMessage err = new NetworkMessage(MessageType.ERROR, "СЕРВЕР", this.currentRoom);
                    err.setErrorMessage("Комната " + roomName + " уже существует или имя некорректно!");
                    sendMessage(err);
                }
                break;

            case HISTORY_REQUEST:
                String targetRoom = msg.getTargetRoom() != null ? msg.getTargetRoom() : this.currentRoom;
                NetworkMessage resp = new NetworkMessage(MessageType.HISTORY_RESPONSE, "СЕРВЕР", targetRoom);
                resp.setHistory(server.getAllRoomHistory(targetRoom));
                resp.setRoomUsers(server.getUsersInRoom(targetRoom));
                resp.setRooms(server.getRoomsList());
                sendMessage(resp);
                break;

            case DISCONNECT:
                close();
                break;

            default:
                break;
        }
    }

    /**
     * Потокобезопасная отправка сообщения клиенту через сокет.
     */
    public synchronized boolean sendMessage(NetworkMessage msg) {
        if (!connected || out == null || socket.isClosed()) return false;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // Сброс кэша ObjectOutputStream для гарантированной отправки обновленных объектов
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    /**
     * Закрывает соединение, сокет и потоки ввода-вывода.
     */
    public synchronized void close() {
        if (!connected) return;
        connected = false;

        server.unregisterClient(this);

        try {
            if (in != null) in.close();
        } catch (Exception ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }

    public Socket getSocket() {
        return socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        updateInfo();
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
        updateInfo();
    }

    private void updateInfo() {
        String ip = socket != null && socket.getInetAddress() != null ?
                socket.getInetAddress().getHostAddress() : "Неизвестно";
        int port = socket != null ? socket.getPort() : 0;
        this.info = new ClientInfo(username, ip, port, currentRoom);
    }

    public ClientInfo getInfo() {
        if (info == null) updateInfo();
        return info;
    }
}
