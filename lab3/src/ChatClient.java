import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;

/**
 * Сетевой клиент чата.
 * Управляет подключением сокета, отправкой сообщений и файлов.
 * Читает входящие сообщения в отдельном потоке.
 */
public class ChatClient {

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onDisconnected(String reason);
    }

    private final String host;
    private final int port;
    private final String username;
    private final MessageListener listener;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread receiverThread;
    private boolean connected = false;

    public ChatClient(String host, int port, String username, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.listener = listener;
    }

    // Подключение к серверу
    public synchronized void connect() throws Exception {
        socket = new Socket(host, port);

        // Создаем потоки сериализации
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        connected = true;

        // Отправляем серверу первое сообщение с именем пользователя
        out.writeObject(new Message(Message.Type.SYSTEM, username, username + " conectat"));
        out.flush();

        // Запускаем фоновый поток для постоянного чтения входящих сообщений
        receiverThread = new Thread(this::listenForMessages, "ClientReceiver-" + username);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    // Фоновый цикл прослушивания входящих сообщений
    private void listenForMessages() {
        try {
            while (connected) {
                Message msg = (Message) in.readObject();
                if (msg != null && listener != null) {
                    listener.onMessageReceived(msg);
                }
            }
        } catch (Exception e) {
            if (connected) {
                disconnect("Conexiunea cu serverul a fost întreruptă.");
            }
        }
    }

    // Отправка обычного текстового сообщения
    public synchronized void sendTextMessage(String text) throws Exception {
        if (!connected) throw new IllegalStateException("Nu sunteți conectat la server!");
        Message msg = new Message(Message.Type.TEXT, username, text);
        out.writeObject(msg);
        out.flush();
    }

    // Отправка ответа на сообщение (Reply)
    public synchronized void sendReplyMessage(String text, String replyAuthor, String replyText) throws Exception {
        if (!connected) throw new IllegalStateException("Nu sunteți conectat la server!");
        Message msg = Message.createReply(username, text, replyAuthor, replyText);
        out.writeObject(msg);
        out.flush();
    }

    // Отправка файла по сети
    public synchronized void sendFile(File file) throws Exception {
        if (!connected) throw new IllegalStateException("Nu sunteți conectat la server!");
        if (file.length() > 50 * 1024 * 1024) { // Ограничение 50 МБ
            throw new IllegalArgumentException("Fișierul este prea mare (maxim 50 MB)!");
        }

        byte[] data = Files.readAllBytes(file.toPath());
        Message fileMsg = Message.createFileMessage(username, file.getName(), data);
        out.writeObject(fileMsg);
        out.flush();
    }

    // Отключение от сервера
    public synchronized void disconnect(String reason) {
        if (!connected) return;
        connected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
        }

        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }
}
