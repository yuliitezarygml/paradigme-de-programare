import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Многопоточный сервер локального чата (Chat-room).
 * Принимает входящие подключения клиентов и рассылает сообщения всем участникам (Broadcast).
 */
public class Server implements Runnable {

    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private Thread serverThread;

    public Server(int port) {
        this.port = port;
    }

    // Запуск сервера в фоновом потоке
    public synchronized void start() {
        if (!running) {
            running = true;
            serverThread = new Thread(this, "ChatServer-Thread");
            serverThread.start();
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[SERVER] Serverul a pornit pe portul " + port);

            while (running) {
                // Ожидаем подключения нового клиента
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] Conexiune nouă de la: " + clientSocket.getRemoteSocketAddress());

                // Создаем и запускаем поток для нового клиента
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                handler.start();
            }

        } catch (Exception e) {
            if (running) {
                System.out.println("[SERVER] Eroare server: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    // Рассылка сообщения ВСЕМ клиентам в комнате (Broadcast)
    public void broadcast(Message message) {
        System.out.println("[BROADCAST] " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Рассылка актуального списка онлайн пользователей
    public void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            sb.append(clients.get(i).getUsername());
            if (i < clients.size() - 1) {
                sb.append(",");
            }
        }
        Message listMsg = Message.createUserListMessage(sb.toString());
        for (ClientHandler client : clients) {
            client.sendMessage(listMsg);
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    // Остановка сервера
    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            clients.clear();
            System.out.println("[SERVER] Serverul a fost oprit.");
        } catch (Exception ignored) {
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getClientCount() {
        return clients.size();
    }

    // Точка входа для запуска отдельного консольного сервера (если нужно)
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        Server server = new Server(port);
        server.start();
    }
}
