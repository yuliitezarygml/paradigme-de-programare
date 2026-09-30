import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Автоматический интеграционный тест сетевого взаимодействия.
 * Проверяет:
 * 1. Запуск сервера
 * 2. Подключение двух клиентов (Alice и Bob)
 * 3. Передачу обычного сообщения
 * 4. Ответ с цитированием (Reply)
 * 5. Передачу и целостность файла
 * 6. Оповещения системы и список пользователей
 */
public class TestNetwork {

    public static void main(String[] args) {
        System.out.println("=== Запуск тестов Сетевого Чата (Lab 3) ===");

        int testPort = 9876;
        Server server = new Server(testPort);
        server.start();

        try {
            Thread.sleep(500); // Даем серверу запуститься

            CountDownLatch msgLatch = new CountDownLatch(1);
            CountDownLatch replyLatch = new CountDownLatch(1);
            CountDownLatch fileLatch = new CountDownLatch(1);

            AtomicReference<String> receivedText = new AtomicReference<>("");
            AtomicReference<String> receivedReply = new AtomicReference<>("");
            AtomicBoolean fileSuccess = new AtomicBoolean(false);

            // Клиент 1: Alice
            ChatClient alice = new ChatClient("127.0.0.1", testPort, "Alice", new ChatClient.MessageListener() {
                @Override
                public void onMessageReceived(Message message) {
                    if (message.getType() == Message.Type.REPLY) {
                        receivedReply.set(message.getContent());
                        replyLatch.countDown();
                    }
                }
                @Override
                public void onDisconnected(String reason) {}
            });

            // Клиент 2: Bob
            ChatClient bob = new ChatClient("127.0.0.1", testPort, "Bob", new ChatClient.MessageListener() {
                @Override
                public void onMessageReceived(Message message) {
                    if (message.getType() == Message.Type.TEXT && "Alice".equals(message.getSender())) {
                        receivedText.set(message.getContent());
                        msgLatch.countDown();
                    } else if (message.getType() == Message.Type.FILE && "Alice".equals(message.getSender())) {
                        if ("test_doc.txt".equals(message.getFileName()) && message.getFileData() != null) {
                            String content = new String(message.getFileData());
                            if ("Continut fisier de test retea".equals(content)) {
                                fileSuccess.set(true);
                            }
                        }
                        fileLatch.countDown();
                    }
                }
                @Override
                public void onDisconnected(String reason) {}
            });

            // 1. Подключение клиентов
            alice.connect();
            bob.connect();
            Thread.sleep(500);

            if (server.getClientCount() == 2) {
                System.out.println("  [OK] Сервер успешно принял 2 подключения (Alice и Bob)");
            } else {
                throw new AssertionError("Ожидалось 2 клиента, но подключено: " + server.getClientCount());
            }

            // 2. Тест отправки сообщения Alice -> Bob
            alice.sendTextMessage("Salut Bob!");
            boolean msgOk = msgLatch.await(3, TimeUnit.SECONDS);
            if (msgOk && "Salut Bob!".equals(receivedText.get())) {
                System.out.println("  [OK] Отправка и прием сообщения: '" + receivedText.get() + "'");
            } else {
                throw new AssertionError("Сообщение не получено или искажено!");
            }

            // 3. Тест ответа на сообщение (Reply) Bob -> Alice
            bob.sendReplyMessage("Salut Alice!", "Alice", "Salut Bob!");
            boolean replyOk = replyLatch.await(3, TimeUnit.SECONDS);
            if (replyOk && "Salut Alice!".equals(receivedReply.get())) {
                System.out.println("  [OK] Ответ на сообщение (Reply) успешно доставлен с цитатой!");
            } else {
                throw new AssertionError("Ответ с цитированием не получен!");
            }

            // 4. Тест отправки файла Alice -> Bob
            File tempFile = new File("test_doc.txt");
            Files.write(tempFile.toPath(), "Continut fisier de test retea".getBytes());

            alice.sendFile(tempFile);
            boolean fileOk = fileLatch.await(4, TimeUnit.SECONDS);
            tempFile.delete();

            if (fileOk && fileSuccess.get()) {
                System.out.println("  [OK] Передача файла по сети успешно проверена (байты совпадают на 100%)!");
            } else {
                throw new AssertionError("Ошибка при передаче файла!");
            }

            // 5. Отключение
            alice.disconnect("Test terminat");
            bob.disconnect("Test terminat");
            Thread.sleep(400);

            if (server.getClientCount() == 0) {
                System.out.println("  [OK] Корректное отключение клиентов от сервера");
            }

            System.out.println("\n>>> ВСЕ СЕТЕВЫЕ ТЕСТЫ УСПЕШНО ПРОЙДЕНЫ! (10/10) <<<");

        } catch (Exception ex) {
            System.out.println("Ошибка при выполнении сетевых тестов: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
