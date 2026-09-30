package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Pachetul principal de date transmis prin rețea între Client și Server.
 * Încorporează suport pentru:
 * - a. Mesagerie text normală
 * - b. Istoric mesaje
 * - c. Răspuns (Reply) la mesaj
 * - d. Transfer de fișiere (nume, dimensiune, date binare)
 * - e. Camere de chat (listă camere, cameră țintă)
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final String id;
    private final MessageType type;
    private String sender;
    private String targetRoom;
    private String text;
    private final long timestamp;

    // Câmpuri pentru Reply (Răspuns la un mesaj primit)
    private String replyToId;
    private String replyToAuthor;
    private String replyToSnippet;

    // Câmpuri pentru File Transfer
    private String fileName;
    private long fileSize;
    private byte[] fileData;

    // Câmpuri pentru sincronizare stare
    private List<ChatRoom> rooms;
    private List<String> roomUsers;
    private List<NetworkMessage> history;
    private String errorMessage;

    public NetworkMessage(MessageType type, String sender, String targetRoom) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.sender = sender;
        this.targetRoom = targetRoom;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Metode Fabrică (Factory Methods) convenabile ---

    public static NetworkMessage createTextMessage(String sender, String targetRoom, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_MESSAGE, sender, targetRoom);
        msg.setText(text);
        return msg;
    }

    public static NetworkMessage createReplyMessage(String sender, String targetRoom, String text,
                                                    String replyToId, String replyToAuthor, String replyToSnippet) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_MESSAGE, sender, targetRoom);
        msg.setText(text);
        msg.setReplyToId(replyToId);
        msg.setReplyToAuthor(replyToAuthor);
        msg.setReplyToSnippet(replyToSnippet);
        return msg;
    }

    public static NetworkMessage createFileMessage(String sender, String targetRoom,
                                                   String fileName, long fileSize, byte[] fileData,
                                                   String replyToId, String replyToAuthor, String replyToSnippet) {
        NetworkMessage msg = new NetworkMessage(MessageType.FILE_TRANSFER, sender, targetRoom);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg.setFileData(fileData);
        msg.setReplyToId(replyToId);
        msg.setReplyToAuthor(replyToAuthor);
        msg.setReplyToSnippet(replyToSnippet);
        return msg;
    }

    public static NetworkMessage createNotification(String targetRoom, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.SERVER_NOTIFICATION, "SISTEM", targetRoom);
        msg.setText(text);
        return msg;
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTargetRoom() {
        return targetRoom;
    }

    public void setTargetRoom(String targetRoom) {
        this.targetRoom = targetRoom;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public boolean isReply() {
        return replyToAuthor != null && !replyToAuthor.isEmpty();
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToAuthor() {
        return replyToAuthor;
    }

    public void setReplyToAuthor(String replyToAuthor) {
        this.replyToAuthor = replyToAuthor;
    }

    public String getReplyToSnippet() {
        return replyToSnippet;
    }

    public void setReplyToSnippet(String replyToSnippet) {
        this.replyToSnippet = replyToSnippet;
    }

    public boolean isFile() {
        return type == MessageType.FILE_TRANSFER && fileName != null;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    public List<ChatRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<ChatRoom> rooms) {
        this.rooms = rooms;
    }

    public List<String> getRoomUsers() {
        return roomUsers;
    }

    public void setRoomUsers(List<String> roomUsers) {
        this.roomUsers = roomUsers;
    }

    public List<NetworkMessage> getHistory() {
        return history;
    }

    public void setHistory(List<NetworkMessage> history) {
        this.history = history;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] (" + targetRoom + ") " + sender + ": " +
                (isFile() ? "[Fișier: " + fileName + " (" + getFormattedFileSize() + ")]" : text);
    }
}
