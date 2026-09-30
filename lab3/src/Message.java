import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс сетевого сообщения.
 * Реализует Serializable для передачи объекта целиком через ObjectOutputStream / ObjectInputStream.
 * Принцип ООП: Инкапсуляция (Encapsulation).
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Типы сообщений
    public enum Type {
        TEXT,         // Обычное текстовое сообщение
        REPLY,        // Ответ с цитированием предыдущего сообщения
        FILE,         // Передача файла
        SYSTEM,       // Системное оповещение (вошел/вышел из чата)
        USER_LIST     // Обновление списка онлайн-пользователей
    }

    private final Type type;
    private final String sender;
    private final String content;
    private final String time;

    // Поля для ответа (Reply)
    private String replyToAuthor;
    private String replyToText;

    // Поля для передачи файлов (File transfer)
    private String fileName;
    private long fileSize;
    private byte[] fileData;

    // Конструктор для текстовых и системных сообщений
    public Message(Type type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    // Конструктор для ответа на сообщение (Reply)
    public static Message createReply(String sender, String content, String replyAuthor, String replyText) {
        Message msg = new Message(Type.REPLY, sender, content);
        msg.replyToAuthor = replyAuthor;
        msg.replyToText = replyText;
        return msg;
    }

    // Конструктор для передачи файла
    public static Message createFileMessage(String sender, String fileName, byte[] data) {
        Message msg = new Message(Type.FILE, sender, "Fișier atașat: " + fileName);
        msg.fileName = fileName;
        msg.fileData = data;
        msg.fileSize = data != null ? data.length : 0;
        return msg;
    }

    // Конструктор списка пользователей онлайн
    public static Message createUserListMessage(String usersString) {
        return new Message(Type.USER_LIST, "SERVER", usersString);
    }

    // Геттеры
    public Type getType() { return type; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getTime() { return time; }
    public String getReplyToAuthor() { return replyToAuthor; }
    public String getReplyToText() { return replyToText; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public byte[] getFileData() { return fileData; }

    @Override
    public String toString() {
        if (type == Type.REPLY) {
            return String.format("[%s] %s (răspuns la %s: \"%s\"): %s", time, sender, replyToAuthor, replyToText, content);
        } else if (type == Type.FILE) {
            return String.format("[%s] %s a trimis fișierul: %s (%.1f KB)", time, sender, fileName, fileSize / 1024.0);
        } else if (type == Type.SYSTEM) {
            return String.format("[%s] *** %s ***", time, content);
        }
        return String.format("[%s] %s: %s", time, sender, content);
    }
}
