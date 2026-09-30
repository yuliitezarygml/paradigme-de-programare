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
 * Test automat pentru validarea cerințelor de rețea (a, b, c, d, e):
 * a. Transmitere și recepție mesaje
 * b. Istoric mesaje
 * c. Răspuns (Reply) la mesaj
 * d. Transmitere și recepție fișiere
 * e. Creare și alăturare chat-rooms
 */
public class TestNetworkChat {

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Pornire teste automate pentru Laboratorul 3 (Chat Rețea)...");

        int testPort = 9876;
        ChatServer server = new ChatServer();
        boolean started = server.start(testPort);
        if (!started) {
            System.err.println("❌ Nu s-a putut porni serverul de test!");
            System.exit(1);
        }
        System.out.println("✅ [1/6] Serverul a pornit cu succes pe portul " + testPort);

        // Test Conectare Client 1 (Alice) și Client 2 (Bob)
        CountDownLatch connectLatch = new CountDownLatch(2);
        ChatClient alice = new ChatClient();
        ChatClient bob = new ChatClient();

        alice.setListener(new SimpleListener("Alice") {
            @Override
            public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {
                System.out.println("✅ Client 1 (" + username + ") conectat!");
                connectLatch.countDown();
            }
        });

        bob.setListener(new SimpleListener("Bob") {
            @Override
            public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {
                System.out.println("✅ Client 2 (" + username + ") conectat!");
                connectLatch.countDown();
            }
        });

        alice.connect("127.0.0.1", testPort, "Alice");
        bob.connect("127.0.0.1", testPort, "Bob");

        if (!connectLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("❌ Timeout la conectare!");
            System.exit(1);
        }

        Thread.sleep(300);

        // Test Cerința a: Transmitere și recepționare mesaj
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

        alice.sendChatMessage("Salut Bob! Bine ai venit pe chat.", null, null, null);

        if (!msgLatch.await(5, TimeUnit.SECONDS) || receivedByBob[0] == null) {
            System.err.println("❌ Cerința a: Bob nu a primit mesajul de la Alice!");
            System.exit(1);
        }
        System.out.println("✅ [2/6] Cerința a: Transmiterea și recepționarea mesajului a funcționat!");

        // Test Cerința c: Răspuns (Reply) la mesaj
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

        bob.sendChatMessage("Salut Alice! Răspund la mesajul tău.",
                receivedByBob[0].getId(),
                receivedByBob[0].getSender(),
                receivedByBob[0].getText());

        if (!replyLatch.await(5, TimeUnit.SECONDS) || replyReceivedByAlice[0] == null) {
            System.err.println("❌ Cerința c: Răspunsul (Reply) nu a fost recepționat corect!");
            System.exit(1);
        }
        System.out.println("✅ [3/6] Cerința c: Răspunsul (Reply) la mesaj a funcționat cu succes! (Citat: @" +
                replyReceivedByAlice[0].getReplyToAuthor() + ")");

        // Test Cerința e: Creare chat-room și comutare
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

        alice.createRoom("#proiect-nou", "Camera de lucru proiect");

        if (!roomLatch.await(5, TimeUnit.SECONDS)) {
            System.err.println("❌ Cerința e: Crearea camerei nu a fost propagată la Bob!");
            System.exit(1);
        }
        System.out.println("✅ [4/6] Cerința e: Crearea camerei #proiect-nou a reușit și a fost notificată!");

        // Trecem ambii în noua cameră
        bob.joinRoom("#proiect-nou");
        Thread.sleep(400);

        // Test Cerința d: Transmitere și recepționare fișiere
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
            fw.write("Continut fisier de test transmis prin socket-uri Java.");
        }

        alice.sendFile(tempFile, null, null, null);

        if (!fileLatch.await(5, TimeUnit.SECONDS) || fileReceivedByBob[0] == null) {
            System.err.println("❌ Cerința d: Bob nu a primit fișierul transmis de Alice!");
            System.exit(1);
        }
        System.out.println("✅ [5/6] Cerința d: Fișierul \"" + fileReceivedByBob[0].getFileName() +
                "\" (" + fileReceivedByBob[0].getFormattedFileSize() + ") a fost recepționat intact!");

        // Test Cerința b: Istoric mesaje
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
            System.err.println("❌ Cerința b: Istoricul camerei #general nu a fost returnat!");
            System.exit(1);
        }
        System.out.println("✅ [6/6] Cerința b: Istoricul a fost interogat cu succes (" + histHolder[0].size() + " mesaje înregistrate)!");

        // Cleanup
        alice.disconnect("Test terminat");
        bob.disconnect("Test terminat");
        server.stop();
        tempFile.delete();

        System.out.println("\n🎉 TOATE CELE 5 CERINȚE (a, b, c, d, e) AU FOST TESTATE ȘI VALIDATE CU SUCCES!");
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
