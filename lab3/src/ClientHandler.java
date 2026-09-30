import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Поток сервера для обслуживания одного подключенного клиента.
 * Читает входящие сообщения и передает их серверу для рассылки всем остальным.
 * Принцип: Многопоточность (Multithreading).
 */
public class ClientHandler extends Thread {

    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username = "Anonim";
    private boolean running = true;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // ВАЖНО: сначала создаем out и делаем flush, потом in, чтобы сокеты не зависали!
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Первое сообщение от клиента — это его имя (никнейм)
            Message firstMessage = (Message) in.readObject();
            this.username = firstMessage.getSender();

            // Оповещаем всех о подключении нового участника
            server.broadcast(new Message(Message.Type.SYSTEM, "SERVER", username + " s-a conectat la chat!"));
            server.broadcastUserList();

            // Цикл чтения входящих сообщений от этого клиента
            while (running) {
                Message msg = (Message) in.readObject();
                if (msg != null) {
                    // Рассылаем полученное сообщение всем в комнате
                    server.broadcast(msg);
                }
            }

        } catch (Exception e) {
            // Клиент отключился или произошел сбой сети
        } finally {
            disconnect();
        }
    }

    // Отправка сообщения конкретно этому клиенту
    public synchronized void sendMessage(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (Exception e) {
            disconnect();
        }
    }

    public void disconnect() {
        running = false;
        server.removeClient(this);
        server.broadcast(new Message(Message.Type.SYSTEM, "SERVER", username + " a părăsit chat-ul."));
        server.broadcastUserList();

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

    public String getUsername() {
        return username;
    }
}
