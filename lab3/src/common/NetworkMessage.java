package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Основной пакет данных, передаваемый по сети между Клиентом и Сервером.
 * Включает полную поддержку всех требований лабораторной работы №3:
 * - a. Обычные текстовые сообщения
 * - b. История сообщений комнаты
 * - c. Ответ (Reply / цитирование) на конкретное сообщение
 * - d. Передача и прием бинарных файлов (имя, размер, массив байтов)
 * - e. Управление комнатами чата (список комнат, целевая комната)
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    // Уникальный идентификатор каждого сообщения (UUID)
    private final String id;
    // Тип сообщения (см. перечисление MessageType)
    private final MessageType type;
    // Имя отправителя
    private String sender;
    // Целевая комната чата (например: #general)
    private String targetRoom;
    // Текст сообщения
    private String text;
    // Временная метка создания сообщения
    private final long timestamp;

    // Поля для реализации ответа на сообщение (Reply / цитирование)
    private String replyToId;       // ID исходного сообщения, на которое отвечаем
    private String replyToAuthor;   // Автор исходного сообщения
    private String replyToSnippet;  // Фрагмент исходного текста для предварительного просмотра цитаты

    // Поля для передачи файлов
    private String fileName;        // Имя передаваемого файла
    private long fileSize;          // Размер файла в байтах
    private byte[] fileData;        // Бинарное содержимое файла

    // Поля для синхронизации состояния между сервером и клиентом
    private List<ChatRoom> rooms;           // Список активных комнат
    private List<String> roomUsers;         // Список пользователей в текущей комнате
    private List<NetworkMessage> history;   // История сообщений комнаты
    private String errorMessage;            // Текст ошибки (если type == ERROR)

    public NetworkMessage(MessageType type, String sender, String targetRoom) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.sender = sender;
        this.targetRoom = targetRoom;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Фабричные методы (Factory Methods) для удобного создания сообщений ---

    /**
     * Создает стандартное текстовое сообщение.
     */
    public static NetworkMessage createTextMessage(String sender, String targetRoom, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_MESSAGE, sender, targetRoom);
        msg.setText(text);
        return msg;
    }

    /**
     * Создает сообщение-ответ (Reply) с прикрепленной цитатой исходного сообщения.
     */
    public static NetworkMessage createReplyMessage(String sender, String targetRoom, String text,
                                                    String replyToId, String replyToAuthor, String replyToSnippet) {
        NetworkMessage msg = new NetworkMessage(MessageType.CHAT_MESSAGE, sender, targetRoom);
        msg.setText(text);
        msg.setReplyToId(replyToId);
        msg.setReplyToAuthor(replyToAuthor);
        msg.setReplyToSnippet(replyToSnippet);
        return msg;
    }

    /**
     * Создает сообщение для передачи файла (с возможностью цитирования).
     */
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

    /**
     * Создает системное уведомление для комнаты (вход/выход пользователей, создание комнаты и т.д.).
     */
    public static NetworkMessage createNotification(String targetRoom, String text) {
        NetworkMessage msg = new NetworkMessage(MessageType.SERVER_NOTIFICATION, "СИСТЕМА", targetRoom);
        msg.setText(text);
        return msg;
    }

    // --- Геттеры и сеттеры ---

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

    /**
     * Возвращает форматированный размер файла (Б, КБ, МБ).
     */
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
                (isFile() ? "[Файл: " + fileName + " (" + getFormattedFileSize() + ")]" : text);
    }
}
