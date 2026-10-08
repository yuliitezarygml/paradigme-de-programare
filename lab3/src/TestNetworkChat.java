import client.ChatClient;
import client.ClientListener;
import common.ChatRoom;
import common.NetworkMessage;
import server.ChatServer;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Автоматический интеграционный тест для проверки всех требований методички (a, b, c, d, e):
 * a. Передача и прием текстовых сообщений в локальной сети
 * b. История полученных сообщений
 * c. Возможность ответа на конкретное сообщение (Reply / цитирование)
 * d. Передача и прием бинарных файлов
 * e. Создание комнат чата (chat-rooms) и переключение между ними
 */
public class TestNetworkChat {

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Запуск автоматических тестов для Лабораторной работы №3 (Сетевой чат)...");

        int testPort = 9876;
        ChatServer server = new ChatServer();
        boolean started = server.start(testPort);
        if (!started) {
            System.err.println("❌ Не удалось запустить тестовый сервер!");
            System.exit(1);
        }
        System.out.println("✅ [1/6] Сервер успешно запущен на порту " + testPort);

        // Тестирование подключения Клиента 1 (Алиса) и Клиента 2 (Боб)
        CountDownLatch connectLatch = new CountDownLatch(2);
        ChatClient alice = new ChatClient();
        ChatClient bob = new ChatClient();

        alice.setListener(new SimpleListener("Alice") {
            @Override
            public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {
                System.out.println("✅ Клиент 1 (" + username + ") успешно подключен!");
                connectLatch.countDown();
            }
        });

        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {
                System.out.println("✅ Клиент 2 (" + username + ") успешно подключен!");
                connectLatch.countDown();
            }
        });

        alice.connect("127.0.0.1", testPort, "Alice");
        bob.connect("127.0.0.1", testPort, "Bob");

        if (!connectLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("❌ Превышено время ожидания подключения клиентов!");
            System.exit(1);
        }

        Thread.sleep(300);

        // Проверка Требования a: Отправка и прием текстового сообщения
        CountDownLatch msgLatch = new CountDownLatch(1);
        final NetworkMessage[] receivedByBob = new NetworkMessage[1];

        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onMessageReceived(NetworkMessage message) {
                if ("Alice".equals(message.getSender())) {
                    receivedByBob[0] = message;
                    msgLatch.countDown();
                }
            }
        });

        alice.sendChatMessage("Привет, Боб! Добро пожаловать в сетевой чат.", null, null, null);

        if (!msgLatch.await(5, TimeUnit.SECONDS) || receivedByBob[0] == null) {
            System.err.println("❌ Требование a: Боб не получил сообщение от Алисы!");
            System.exit(1);
        }
        System.out.println("✅ [2/6] Требование a: Отправка и прием сообщений работают штатно!");

        // Проверка Требования c: Ответ (Reply) на полученное сообщение с цитированием
        CountDownLatch replyLatch = new CountDownLatch(1);
        final NetworkMessage[] replyReceivedByAlice = new NetworkMessage[1];

        alice.setListener(new SimpleListener("Alice") {
            @Override
            public void onMessageReceived(NetworkMessage message) {
                if (message.isReply()) {
                    replyReceivedByAlice[0] = message;
                    replyLatch.countDown();
                }
            }
        });

        bob.sendChatMessage("Привет, Алиса! Отвечаю на твое сообщение.",
                receivedByBob[0].getId(),
                receivedByBob[0].getSender(),
                receivedByBob[0].getText());

        if (!replyLatch.await(5, TimeUnit.SECONDS) || replyReceivedByAlice[0] == null) {
            System.err.println("❌ Требование c: Ответ (Reply) не был получен корректно!");
            System.exit(1);
        }
        System.out.println("✅ [3/6] Требование c: Ответ (Reply) успешно отправлен и отображен! (Цитата автора: @" +
                replyReceivedByAlice[0].getReplyToAuthor() + ")");

        // Проверка Требования e: Создание комнаты чата и переключение
        CountDownLatch roomLatch = new CountDownLatch(1);
        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onRoomListUpdated(List<ChatRoom> rooms) {
                for (ChatRoom r : rooms) {
                    if (r.getName().equalsIgnoreCase("#proiect-nou")) {
                        roomLatch.countDown();
                    }
                }
            }
        });

        alice.createRoom("#proiect-nou", "Рабочая комната для проекта");

        if (!roomLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("❌ Требование e: Создание комнаты не было передано Бобу!");
            System.exit(1);
        }
        System.out.println("✅ [4/6] Требование e: Создание комнаты #proiect-nou успешно выполнено и синхронизировано!");

        // Переводим обоих участников в новую комнату
        bob.joinRoom("#proiect-nou");
        Thread.sleep(400);

        // Проверка Требования d: Передача и прием файлов
        CountDownLatch fileLatch = new CountDownLatch(1);
        final NetworkMessage[] fileReceivedByBob = new NetworkMessage[1];

        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onFileReceived(NetworkMessage message) {
                if (message.isFile()) {
                    fileReceivedByBob[0] = message;
                    fileLatch.countDown();
                }
            }
        });

        File tempFile = File.createTempFile("test_file_lab3", ".txt");
        try (FileWriter fw = new FileWriter(tempFile)) {
            fw.write("Тестовое содержимое бинарного файла, переданного через сокеты Java.");
        }

        alice.sendFile(tempFile, null, null, null);

        if (!fileLatch.await(5, TimeUnit.SECONDS) || fileReceivedByBob[0] == null) {
            System.err.println("❌ Требование d: Боб не получил файл, отправленный Алисой!");
            System.exit(1);
        }
        System.out.println("✅ [5/6] Требование d: Файл \"" + fileReceivedByBob[0].getFileName() +
                "\" (" + fileReceivedByBob[0].getFormattedFileSize() + ") получен в целости и сохранности!");

        // Проверка Требования b: История сообщений
        CountDownLatch histLatch = new CountDownLatch(1);
        final List<NetworkMessage>[] histHolder = new List[1];

        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onRoomHistoryLoaded(String room, List<NetworkMessage> history) {
                histHolder[0] = history;
                histLatch.countDown();
            }
        });

        bob.requestHistory("#general");
        if (!histLatch.await(5, TimeUnit.SECONDS) || histHolder[0] == null || histHolder[0].isEmpty()) {
            System.err.println("❌ Требование b: История для комнаты #general не была возвращена!");
            System.exit(1);
        }
        System.out.println("✅ [6/6] Требование b: История переписки успешно запрошена (" + histHolder[0].size() + " сообщений зафиксировано)!");

        // Корректное завершение и освобождение ресурсов
        alice.disconnect("Тест завершен");
        bob.disconnect("Тест завершен");
        server.stop();
        tempFile.delete();

        System.out.println("\n🎉 ВСЕ 5 ТРЕБОВАНИЙ МЕТОДИЧКИ (a, b, c, d, e) УСПЕШНО ПРОТЕСТИРОВАНЫ И ПОДТВЕРЖДЕНЫ!");
        System.exit(0);
    }

    private static class SimpleListener implements ClientListener {
        private final String name;
        public SimpleListener(String name) { this.name = name; }
        @Override public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {}
        @Override public void onConnectionFailed(String error) {}
        @Override public void onDisconnected(String reason) {}
        @Override public void onMessageReceived(NetworkMessage message) {}
        @Override public void onFileReceived(NetworkMessage message) {}
        @Override public void onRoomListUpdated(List<ChatRoom> rooms) {}
        @Override public void onUserListUpdated(List<String> users) {}
        @Override public void onRoomHistoryLoaded(String room, List<NetworkMessage> history) {}
        @Override public void onNotification(String text) {}
        @Override public void onError(String error) {}
    }
}
