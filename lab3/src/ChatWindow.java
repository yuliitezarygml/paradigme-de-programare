import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Графический интерфейс чата на Java Swing.
 * Полноценная реализация требований лабораторной работы №3:
 * - Отправка и прием сообщений (в реальном времени)
 * - История сообщений с отметками времени и сохранением в файл
 * - Ответ на сообщение (Reply / Цитата)
 * - Отправка и прием файлов (с возможностью сохранения на диск)
 * - Комната чата (Chat-room) со списком онлайн участников
 */
public class ChatWindow extends JFrame implements ChatClient.MessageListener {

    private ChatClient client;
    private Server localServer; // Возможность запустить сервер прямо из этого окна

    // Компоненты подключения
    private JTextField hostField;
    private JTextField portField;
    private JTextField userField;
    private JButton btnConnect;
    private JButton btnStartServer;

    // Компоненты чата
    private JTextPane chatPane;
    private StyledDocument chatDoc;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    // Панель ответа на сообщение (Reply)
    private JPanel replyBar;
    private JLabel replyLabel;
    private Message selectedMessageForReply = null;

    // Поле ввода и кнопки
    private JTextField inputField;
    private JButton btnSend;
    private JButton btnSendFile;
    private JButton btnHistory;

    // Список всех полученных сообщений (для истории и reply)
    private final List<Message> messageHistory = new ArrayList<>();

    public ChatWindow() {
        setTitle("Rețeaua locală - Chat & File Transfer (Laborator 3)");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(5, 5));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));

        // 1. Верхняя панель подключения
        JPanel topPanel = createTopPanel();
        root.add(topPanel, BorderLayout.NORTH);

        // 2. Центральная панель: Чат слева, Список пользователей справа
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.8);

        // Окно чата
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatDoc = chatPane.getStyledDocument();
        JScrollPane chatScroll = new JScrollPane(chatPane);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Mesaje în rețea (Chat Room)"));

        // Контекстное меню для сообщений (клик правой кнопкой мыши для ответа)
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem itemReply = new JMenuItem("Răspunde la acest mesaj (Reply)");
        itemReply.addActionListener(e -> setupReplyFromSelection());
        contextMenu.add(itemReply);
        chatPane.setComponentPopupMenu(contextMenu);

        centerSplit.setLeftComponent(chatScroll);

        // Список онлайн пользователей
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("Utilizatori online"));
        userScroll.setPreferredSize(new Dimension(180, 0));
        centerSplit.setRightComponent(userScroll);

        root.add(centerSplit, BorderLayout.CENTER);

        // 3. Нижняя панель: Плашка цитирования (Reply) + поле ввода + кнопки
        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));

        // Панель цитирования (Reply bar)
        replyBar = new JPanel(new BorderLayout(5, 0));
        replyBar.setBackground(new Color(230, 242, 255));
        replyBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        replyBar.setVisible(false);

        replyLabel = new JLabel("Răspuns la: ");
        replyLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        replyLabel.setForeground(new Color(30, 64, 175));
        replyBar.add(replyLabel, BorderLayout.CENTER);

        JButton btnCancelReply = new JButton("×");
        btnCancelReply.setMargin(new Insets(0, 4, 0, 4));
        btnCancelReply.setFocusable(false);
        btnCancelReply.addActionListener(e -> cancelReply());
        replyBar.add(btnCancelReply, BorderLayout.EAST);

        bottomContainer.add(replyBar);

        // Панель ввода сообщения
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setEnabled(false);
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        btnSend = new JButton("Trimite 📨");
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> sendMessage());

        btnSendFile = new JButton("Fișier 📎");
        btnSendFile.setEnabled(false);
        btnSendFile.setToolTipText("Trimite un fișier în rețea");
        btnSendFile.addActionListener(e -> sendFile());

        btnHistory = new JButton("Istoric 💾");
        btnHistory.setToolTipText("Salvează sau vizualizează istoricul mesajelor");
        btnHistory.addActionListener(e -> exportHistory());

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionBtns.add(btnSendFile);
        actionBtns.add(btnHistory);
        actionBtns.add(btnSend);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(actionBtns, BorderLayout.EAST);

        bottomContainer.add(Box.createVerticalStrut(4));
        bottomContainer.add(inputPanel);

        root.add(bottomContainer, BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 4, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Configurare Rețea (Настройка сети)"));

        // Строка 1: Параметры сети (IP, Порт, Имя)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row1.add(new JLabel("IP Server:"));
        hostField = new JTextField("127.0.0.1", 10);
        row1.add(hostField);

        row1.add(new JLabel("Port:"));
        portField = new JTextField("8888", 5);
        row1.add(portField);

        row1.add(new JLabel("Nume (Nick):"));
        userField = new JTextField("User" + (int)(Math.random() * 900 + 100), 10);
        row1.add(userField);

        // Строка 2: Кнопки
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnStartServer = new JButton("1. Pornește Server Local 🖥️");
        btnStartServer.setFont(new Font("Arial", Font.BOLD, 13));
        btnStartServer.addActionListener(e -> toggleLocalServer());
        row2.add(btnStartServer);

        btnConnect = new JButton("2. Conectare la Chat 🔌");
        btnConnect.setFont(new Font("Arial", Font.BOLD, 13));
        btnConnect.addActionListener(e -> toggleConnection());
        row2.add(btnConnect);

        panel.add(row1);
        panel.add(row2);

        return panel;
    }

    // ==========================================
    // Управление подключением
    // ==========================================

    private void toggleConnection() {
        if (client == null || !client.isConnected()) {
            // Подключаемся
            String host = hostField.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Port invalid!", "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String nick = userField.getText().trim();
            if (nick.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Introduceți un nume de utilizator!", "Eroare", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                client = new ChatClient(host, port, nick, this);
                client.connect();
                onConnectSuccess();

            } catch (Exception ex) {
                // Если подключаемся к локальному хосту, но сервер еще не запущен — предлагаем включить его автоматически!
                if ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)) {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "Serverul nu este pornit pe " + host + ":" + port + ".\n" +
                            "Doriți să pornim automat serverul local chiar acum și să ne conectăm?",
                            "Pornire Automată Server", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                    if (choice == JOptionPane.YES_OPTION) {
                        toggleLocalServer();
                        try {
                            Thread.sleep(400); // Даем серверу запуститься
                            client = new ChatClient(host, port, nick, this);
                            client.connect();
                            onConnectSuccess();
                            return;
                        } catch (Exception err) {
                            JOptionPane.showMessageDialog(this, "Eroare la pornirea automată: " + err.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Nu s-a putut conecta la server!\n" +
                            "Asigurați-vă că serverul este pornit.\nDetalii: " + ex.getMessage(), "Eroare Conexiune", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            // Отключаемся
            client.disconnect("Deconectat de utilizator.");
        }
    }

    private void onConnectSuccess() {
        btnConnect.setText("Deconectare ❌");
        btnConnect.setBackground(new Color(254, 202, 202));
        hostField.setEnabled(false);
        portField.setEnabled(false);
        userField.setEnabled(false);
        inputField.setEnabled(true);
        btnSend.setEnabled(true);
        btnSendFile.setEnabled(true);
        inputField.requestFocus();
    }

    private void toggleLocalServer() {
        if (localServer == null || !localServer.isRunning()) {
            int port = 8888;
            try { port = Integer.parseInt(portField.getText().trim()); } catch (Exception ignored) {}
            localServer = new Server(port);
            localServer.start();
            btnStartServer.setText("Oprește Server ⏹️");
            btnStartServer.setBackground(new Color(254, 226, 226));
            appendSystemMessage("Serverul local a fost pornit pe portul " + port + ". Acum vă puteți conecta!");
        } else {
            localServer.stop();
            btnStartServer.setText("Pornește Server Local 🖥️");
            btnStartServer.setBackground(null);
            appendSystemMessage("Serverul local a fost oprit.");
        }
    }

    // ==========================================
    // Отправка сообщений и файлов
    // ==========================================

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null || !client.isConnected()) return;

        try {
            if (selectedMessageForReply != null) {
                // Отправляем как ответ (Reply)
                client.sendReplyMessage(text, selectedMessageForReply.getSender(), selectedMessageForReply.getContent());
                cancelReply();
            } else {
                // Обычное текстовое сообщение
                client.sendTextMessage(text);
            }
            inputField.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Eroare la trimitere: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendFile() {
        if (client == null || !client.isConnected()) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                appendSystemMessage("Se trimite fișierul '" + selectedFile.getName() + "'...");
                client.sendFile(selectedFile);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la trimiterea fișierului: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Подготовка ответа на выбранное сообщение
    private void setupReplyFromSelection() {
        if (messageHistory.isEmpty()) return;

        // Предлагаем пользователю выбрать сообщение из списка последних
        String[] options = new String[Math.min(messageHistory.size(), 10)];
        int startIdx = Math.max(0, messageHistory.size() - 10);
        int j = 0;
        for (int i = startIdx; i < messageHistory.size(); i++) {
            Message m = messageHistory.get(i);
            options[j++] = m.getSender() + ": " + (m.getContent().length() > 30 ? m.getContent().substring(0, 30) + "..." : m.getContent());
        }

        String choice = (String) JOptionPane.showInputDialog(this,
                "Selectați mesajul la care doriți să răspundeți:",
                "Răspunde (Reply)", JOptionPane.QUESTION_MESSAGE, null, options, options[options.length - 1]);

        if (choice != null) {
            for (int i = startIdx; i < messageHistory.size(); i++) {
                Message m = messageHistory.get(i);
                if (choice.startsWith(m.getSender())) {
                    selectedMessageForReply = m;
                    replyLabel.setText("Răspuns la [" + m.getSender() + "]: \"" + m.getContent() + "\"");
                    replyBar.setVisible(true);
                    inputField.requestFocus();
                    break;
                }
            }
        }
    }

    private void cancelReply() {
        selectedMessageForReply = null;
        replyBar.setVisible(false);
    }

    // Сохранение и просмотр истории сообщений в файл
    private void exportHistory() {
        if (messageHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Istoricul este gol!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File historyFile = new File("chat_history.txt");
        try (PrintWriter writer = new PrintWriter(new FileWriter(historyFile, false))) {
            writer.println("=== ISTORIC CHAT - REȚEAUA LOCALĂ ===");
            for (Message m : messageHistory) {
                writer.println(m.toString());
            }
            JOptionPane.showMessageDialog(this,
                    "Istoricul a fost salvat cu succes în fișierul:\n" + historyFile.getAbsolutePath() +
                            "\nTotal mesaje: " + messageHistory.size(),
                    "Salvare Istoric", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Eroare la salvarea istoricului: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Обработка входящих сообщений (MessageListener)
    // ==========================================

    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            messageHistory.add(message);

            switch (message.getType()) {
                case TEXT:
                    appendTextMessage(message);
                    break;
                case REPLY:
                    appendReplyMessage(message);
                    break;
                case FILE:
                    appendFileMessage(message);
                    break;
                case SYSTEM:
                    appendSystemMessage(message.getContent());
                    break;
                case USER_LIST:
                    updateUserList(message.getContent());
                    break;
            }

            // Автопрокрутка вниз
            chatPane.setCaretPosition(chatDoc.getLength());
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            btnConnect.setText("Conectare 🔌");
            btnConnect.setBackground(null);
            hostField.setEnabled(true);
            portField.setEnabled(true);
            userField.setEnabled(true);
            inputField.setEnabled(false);
            btnSend.setEnabled(false);
            btnSendFile.setEnabled(false);
            userListModel.clear();
            cancelReply();
            appendSystemMessage("Deconectat: " + reason);
        });
    }

    // ==========================================
    // Отрисовка сообщений в окне чата (Стилизация)
    // ==========================================

    private void appendTextMessage(Message msg) {
        try {
            boolean isMe = client != null && client.getUsername().equals(msg.getSender());

            SimpleAttributeSet timeAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(timeAttr, Color.GRAY);
            StyleConstants.setFontSize(timeAttr, 11);

            SimpleAttributeSet nameAttr = new SimpleAttributeSet();
            StyleConstants.setBold(nameAttr, true);
            StyleConstants.setForeground(nameAttr, isMe ? new Color(16, 185, 129) : new Color(59, 130, 246));

            SimpleAttributeSet textAttr = new SimpleAttributeSet();
            StyleConstants.setFontSize(textAttr, 14);

            chatDoc.insertString(chatDoc.getLength(), "[" + msg.getTime() + "] ", timeAttr);
            chatDoc.insertString(chatDoc.getLength(), msg.getSender() + ": ", nameAttr);
            chatDoc.insertString(chatDoc.getLength(), msg.getContent() + "\n", textAttr);

        } catch (Exception ignored) {}
    }

    private void appendReplyMessage(Message msg) {
        try {
            SimpleAttributeSet quoteAttr = new SimpleAttributeSet();
            StyleConstants.setItalic(quoteAttr, true);
            StyleConstants.setForeground(quoteAttr, new Color(100, 116, 139));
            StyleConstants.setFontSize(quoteAttr, 12);

            chatDoc.insertString(chatDoc.getLength(), "   ┌─ [Răspuns la " + msg.getReplyToAuthor() + ": \"" + msg.getReplyToText() + "\"]\n", quoteAttr);
            appendTextMessage(msg);

        } catch (Exception ignored) {}
    }

    private void appendFileMessage(Message msg) {
        try {
            SimpleAttributeSet fileAttr = new SimpleAttributeSet();
            StyleConstants.setBold(fileAttr, true);
            StyleConstants.setForeground(fileAttr, new Color(147, 51, 234)); // Фиолетовый

            chatDoc.insertString(chatDoc.getLength(), "[" + msg.getTime() + "] 📎 " + msg.getSender() +
                    " a trimis un fișier: " + msg.getFileName() +
                    String.format(" (%.1f KB)\n", msg.getFileSize() / 1024.0), fileAttr);

            // Добавляем интерактивную кнопку сохранения прямо в чат!
            JButton saveBtn = new JButton("📥 Descarcă " + msg.getFileName());
            saveBtn.setMargin(new Insets(2, 6, 2, 6));
            saveBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            saveBtn.addActionListener(e -> saveReceivedFile(msg));

            chatPane.setCaretPosition(chatDoc.getLength());
            chatPane.insertComponent(saveBtn);
            chatDoc.insertString(chatDoc.getLength(), "\n", null);

        } catch (Exception ignored) {}
    }

    private void saveReceivedFile(Message msg) {
        if (msg.getFileData() == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(msg.getFileName()));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                Files.write(dest.toPath(), msg.getFileData());
                JOptionPane.showMessageDialog(this, "Fișier salvat cu succes în:\n" + dest.getAbsolutePath(), "Descărcare reușită", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la salvarea fișierului: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void appendSystemMessage(String text) {
        try {
            SimpleAttributeSet sysAttr = new SimpleAttributeSet();
            StyleConstants.setItalic(sysAttr, true);
            StyleConstants.setForeground(sysAttr, new Color(180, 83, 9)); // Оранжевый

            chatDoc.insertString(chatDoc.getLength(), "*** " + text + " ***\n", sysAttr);
        } catch (Exception ignored) {}
    }

    private void updateUserList(String rawUsers) {
        userListModel.clear();
        if (rawUsers != null && !rawUsers.isEmpty()) {
            String[] users = rawUsers.split(",");
            for (String u : users) {
                if (!u.trim().isEmpty()) {
                    userListModel.addElement(u.trim());
                }
            }
        }
    }
}
