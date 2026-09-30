package client;

import common.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.List;

/**
 * Client de rețea responsabil de conexiunea socket TCP către Server.
 * Recepționează asincron mesajele și oferă metode pentru transmiterea de:
 * - mesaje text simple
 * - răspunsuri la mesaje (reply)
 * - fișiere atașate
 * - navigare și creare camere de chat
 */
public class ChatClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username = "Anonim";
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
     * Inițiază conexiunea către server pe un fir de execuție separat.
     */
    public void connect(String host, int port, String requestedUsername) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                connected = true;

                // Trimitere cerere inițială de conectare
                NetworkMessage connMsg = new NetworkMessage(MessageType.CONNECT, requestedUsername, "#general");
                connMsg.setText(requestedUsername);
                sendMessageDirect(connMsg);

                // Pornire fir ascultare mesaje sosite
                new Thread(this::receiveLoop, "ChatClient-Receiver").start();

            } catch (Exception e) {
                connected = false;
                if (listener != null) {
                    listener.onConnectionFailed("Eșec la conectare la " + host + ":" + port + " (" + e.getMessage() + ")");
                }
            }
        }, "ChatClient-ConnectThread").start();
    }

    /**
     * Bucla de ascultare a mesajelor primite de la server.
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
            // Conexiunea s-a întrerupt
        } catch (Exception e) {
            if (connected) {
                System.err.println("[ChatClient] Eroare la citirea fluxului de date: " + e.getMessage());
            }
        } finally {
            disconnect("Conexiunea cu serverul a fost pierdută.");
        }
    }

    /**
     * Tratează fiecare pachet primit de la server.
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
                disconnect("Ai fost deconectat de pe server: " + msg.getText());
                break;

            default:
                break;
        }
    }

    /**
     * Transmiterea unui mesaj text sau a unui răspuns (Reply).
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
     * Transmiterea unui fișier binar în camera de chat curentă.
     */
    public boolean sendFile(File file, String replyToId, String replyToAuthor, String replyToSnippet) {
        if (!connected || file == null || !file.exists()) return false;
        try {
            long size = file.length();
            // Verificare limită rezonabilă pentru memorie (ex: 50 MB)
            if (size > 50 * 1024 * 1024) {
                if (listener != null) {
                    listener.onError("Fișierul depășește limita recomandată de 50 MB!");
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
                listener.onError("Eroare la citirea fișierului: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Solicită trecerea într-o altă cameră de chat.
     */
    public void joinRoom(String roomName) {
        if (!connected || roomName == null) return;
        this.currentRoom = roomName;
        NetworkMessage msg = new NetworkMessage(MessageType.JOIN_ROOM, username, roomName);
        sendMessageDirect(msg);
    }

    /**
     * Solicită crearea unei noi camere de chat.
     */
    public void createRoom(String roomName, String description) {
        if (!connected || roomName == null) return;
        NetworkMessage msg = new NetworkMessage(MessageType.CREATE_ROOM, username, currentRoom);
        msg.setText(roomName);
        msg.setReplyToSnippet(description);
        sendMessageDirect(msg);
    }

    /**
     * Solicită istoricul mesajelor pentru o cameră.
     */
    public void requestHistory(String roomName) {
        if (!connected) return;
        NetworkMessage msg = new NetworkMessage(MessageType.HISTORY_REQUEST, username, roomName);
        sendMessageDirect(msg);
    }

    /**
     * Trimite un pachet către server în mod sincronizat.
     */
    private synchronized boolean sendMessageDirect(NetworkMessage msg) {
        if (!connected || out == null || socket.isClosed()) return false;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
            return true;
        } catch (IOException e) {
            disconnect("Eroare la transmiterea datelor către server.");
            return false;
        }
    }

    /**
     * Deconectare controlată.
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
