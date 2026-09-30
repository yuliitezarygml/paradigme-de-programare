package server;

import common.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Gestionează conexiunea socket dedicată pentru un client individual.
 * Rulează pe un fir de execuție separat pentru recepționarea asincronă a mesajelor.
 */
public class ClientHandler implements Runnable {

    private final ChatServer server;
    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username = "Anonim";
    private String currentRoom = "#general";
    private volatile boolean connected = true;
    private ClientInfo info;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Inițializăm fluxurile I/O (ObjectOutputStream înainte de ObjectInputStream conform specificației Java)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Bucla principală de recepție pachete din rețea
            while (connected && !socket.isClosed()) {
                Object obj = in.readObject();
                if (!(obj instanceof NetworkMessage)) continue;

                NetworkMessage msg = (NetworkMessage) obj;
                handleMessage(msg);
            }
        } catch (EOFException | SocketException e) {
            // Clientul a închis conexiunea
        } catch (Exception e) {
            System.err.println("[ClientHandler] Eroare conexiune pentru " + username + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Procesează pachetul primit în funcție de tipul său.
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
                    NetworkMessage err = new NetworkMessage(MessageType.ERROR, "SERVER", this.currentRoom);
                    err.setErrorMessage("Camera " + roomName + " există deja sau numele este invalid!");
                    sendMessage(err);
                }
                break;

            case HISTORY_REQUEST:
                String targetRoom = msg.getTargetRoom() != null ? msg.getTargetRoom() : this.currentRoom;
                NetworkMessage resp = new NetworkMessage(MessageType.HISTORY_RESPONSE, "SERVER", targetRoom);
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
     * Trimite un mesaj către client în mod sincronizat pentru a preveni coliziunile pe stream.
     */
    public synchronized boolean sendMessage(NetworkMessage msg) {
        if (!connected || out == null || socket.isClosed()) return false;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // Curăță cache-ul ObjectOutputStream pentru a permite modificări de stare
            return true;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    /**
     * Închide conexiunea cu clientul.
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
                socket.getInetAddress().getHostAddress() : "Necunoscut";
        int port = socket != null ? socket.getPort() : 0;
        this.info = new ClientInfo(username, ip, port, currentRoom);
    }

    public ClientInfo getInfo() {
        if (info == null) updateInfo();
        return info;
    }
}
