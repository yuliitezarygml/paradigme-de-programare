package server;

import common.*;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Современный графический интерфейс (GUI) панели управления Сервером локальной сети.
 * Предоставляет:
 * - Кнопки Запуск/Остановка сервера и настройку порта
 * - Карточки статистики в реальном времени (Статус, Комнаты, Клиенты, Сообщения, Файлы)
 * - Управление комнатами чата (просмотр, создание, удаление с авто-миграцией)
 * - Мониторинг подключенных клиентов и возможность принудительного отключения (Kick)
 * - Полный аудит событий и общий журнал сообщений с экспортом в файл .log
 * - Отправку глобальных объявлений (Broadcast) во все комнаты
 */
public class ServerGUI extends JFrame implements ServerListener {

    private final ChatServer server;

    // Верхняя панель управления
    private JLabel statusBadge;
    private JLabel ipBadge;
    private JSpinner portSpinner;
    private JButton btnStart;
    private JButton btnStop;

    // Карточки статистики
    private JLabel lblStateVal;
    private JLabel lblRoomsVal;
    private JLabel lblClientsVal;
    private JLabel lblMessagesVal;
    private JLabel lblFilesVal;

    private int totalMessagesCount = 0;
    private int totalFilesCount = 0;

    // Модели и таблицы данных
    private DefaultTableModel roomsTableModel;
    private JTable roomsTable;

    private DefaultTableModel clientsTableModel;
    private JTable clientsTable;

    private DefaultTableModel logsTableModel;
    private JTable logsTable;

    // Поле отправки глобального объявления
    private JTextField tfBroadcast;
    private JButton btnBroadcast;

    public ServerGUI() {
        super("Сервер локального чата - Панель управления (Лабораторная 3)");
        this.server = new ChatServer();
        this.server.setListener(this);

        initUI();
        refreshStats();
        refreshRoomsTable();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 680);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARKER);
        setLayout(new BorderLayout());

        // 1. Верхняя панель управления и состояния
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 2. Центральная область (Карточки статистики + Вкладки управления)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setBackground(UITheme.BG_DARKER);
        centerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Карточки метрик
        JPanel statsPanel = createStatsCardsPanel();
        centerPanel.add(statsPanel, BorderLayout.NORTH);

        // Вкладки (Комнаты, Клиенты, Журнал аудита)
        JTabbedPane tabbedPane = createTabsPanel();
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Нижняя панель глобальных объявлений (Broadcast)
        JPanel bottomPanel = createBroadcastBar();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Создает верхнюю панель с информацией о сервере, локальном IP, порте и кнопками запуска.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(UITheme.BG_SIDEBAR);
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));

        // Слева: Заголовок и подзаголовок
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("⚡ СЕРВЕР ЛОКАЛЬНОГО ЧАТА (ЛАБ 3)");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Мониторинг сокетов, многопоточность, передача файлов и аудит истории");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        panel.add(titlePanel, BorderLayout.WEST);

        // Справа: Бейджи состояния, IP, ввод порта и кнопки Старт/Стоп
        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ctrlPanel.setOpaque(false);

        String localIp = ChatServer.detectLocalIP();
        ipBadge = UITheme.createStatusBadge("IP: " + localIp, UITheme.BG_CARD, UITheme.TEXT_ACCENT);
        statusBadge = UITheme.createStatusBadge("● ОСТАНОВЛЕН", UITheme.DANGER, Color.WHITE);

        JLabel lblPort = new JLabel("Порт:");
        lblPort.setFont(UITheme.FONT_BOLD);
        lblPort.setForeground(UITheme.TEXT_MUTED);

        portSpinner = new JSpinner(new SpinnerNumberModel(8888, 1024, 65535, 1));
        portSpinner.setPreferredSize(new Dimension(80, 32));
        JComponent editor = portSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(UITheme.BG_INPUT);
            tf.setForeground(UITheme.TEXT_PRIMARY);
            tf.setCaretColor(UITheme.TEXT_PRIMARY);
        }

        btnStart = UITheme.createSuccessButton("▶ Запустить Сервер");
        btnStart.addActionListener(e -> startServer());

        btnStop = UITheme.createDangerButton("⏹ Остановить");
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopServer());

        ctrlPanel.add(ipBadge);
        ctrlPanel.add(statusBadge);
        ctrlPanel.add(lblPort);
        ctrlPanel.add(portSpinner);
        ctrlPanel.add(btnStart);
        ctrlPanel.add(btnStop);

        panel.add(ctrlPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * Создает блок из 5 информационных карточек статистики.
     */
    private JPanel createStatsCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 12, 0));
        panel.setOpaque(false);

        lblStateVal = new JLabel("Остановлен");
        lblRoomsVal = new JLabel("3");
        lblClientsVal = new JLabel("0");
        lblMessagesVal = new JLabel("0");
        lblFilesVal = new JLabel("0");

        panel.add(createCard("Статус Сервера", lblStateVal, UITheme.DANGER));
        panel.add(createCard("Активных Комнат", lblRoomsVal, UITheme.ACCENT));
        panel.add(createCard("Клиентов Онлайн", lblClientsVal, UITheme.SUCCESS));
        panel.add(createCard("Всего Сообщений", lblMessagesVal, UITheme.WARNING));
        panel.add(createCard("Передано Файлов", lblFilesVal, UITheme.INFO));

        return panel;
    }

    /**
     * Создает стильную скругленную карточку показателя с акцентной боковой полосой.
     */
    private JPanel createCard(String title, JLabel valLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Тонкая цветная полоса слева
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 12));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_SMALL);
        lblTitle.setForeground(UITheme.TEXT_MUTED);

        valLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        valLabel.setForeground(UITheme.TEXT_PRIMARY);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Создает панель вкладок: Комнаты, Клиенты, Аудит.
     */
    private JTabbedPane createTabsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.BG_SIDEBAR);
        tabs.setForeground(UITheme.TEXT_PRIMARY);
        tabs.setFont(UITheme.FONT_HEADER);

        tabs.addTab("📁 Комнаты ЧАТА", createRoomsTab());
        tabs.addTab("👥 Подключенные Клиенты", createClientsTab());
        tabs.addTab("📜 Журнал & История Аудита", createLogsTab());

        return tabs;
    }

    /**
     * Вкладка управления комнатами чата.
     */
    private JPanel createRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Панель действий над комнатами
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnAddRoom = UITheme.createPrimaryButton("➕ Создать Новую Комнату");
        btnAddRoom.addActionListener(e -> showCreateRoomDialog());

        JButton btnDeleteRoom = UITheme.createDangerButton("🗑️ Удалить Выбранную Комнату");
        btnDeleteRoom.addActionListener(e -> deleteSelectedRoom());

        bar.add(btnAddRoom);
        bar.add(btnDeleteRoom);
        panel.add(bar, BorderLayout.NORTH);

        // Таблица комнат
        String[] cols = {"Название Комнаты", "Описание", "Создатель", "Участников Онлайн"};
        roomsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomsTable = new JTable(roomsTableModel);
        styleTable(roomsTable);

        JScrollPane scroll = new JScrollPane(roomsTable);
        UITheme.applyModernScrollBar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Вкладка мониторинга подключенных клиентов.
     */
    private JPanel createClientsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnKick = UITheme.createDangerButton("⚡ Отключить Клиента (Kick)");
        btnKick.addActionListener(e -> kickSelectedUser());

        bar.add(btnKick);
        panel.add(bar, BorderLayout.NORTH);

        String[] cols = {"Имя Пользователя", "IP-Адрес", "Порт Сокета", "Текущая Комната", "Время Подключения"};
        clientsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        clientsTable = new JTable(clientsTableModel);
        styleTable(clientsTable);

        JScrollPane scroll = new JScrollPane(clientsTable);
        UITheme.applyModernScrollBar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Вкладка детального журнала сервера и аудита сообщений.
     */
    private JPanel createLogsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnExport = UITheme.createSecondaryButton("📥 Экспорт Журнала (.log)");
        btnExport.addActionListener(e -> exportLogs());

        JButton btnClear = UITheme.createSecondaryButton("🧹 Очистить Экран");
        btnClear.addActionListener(e -> logsTableModel.setRowCount(0));

        bar.add(btnExport);
        bar.add(btnClear);
        panel.add(bar, BorderLayout.NORTH);

        String[] cols = {"Время", "Уровень / Тип", "Источник / Клиент", "Содержимое / Детали"};
        logsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        logsTable = new JTable(logsTableModel);
        styleTable(logsTable);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(450);

        JScrollPane scroll = new JScrollPane(logsTable);
        UITheme.applyModernScrollBar(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Нижняя панель для отправки широковещательного системного объявления.
     */
    private JPanel createBroadcastBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(UITheme.BG_SIDEBAR);
        panel.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel lbl = new JLabel("📢 Объявление во все комнаты:");
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.WEST);

        tfBroadcast = UITheme.createTextField("Введите системное объявление, которое увидят все пользователи...");
        tfBroadcast.addActionListener(e -> sendBroadcast());
        panel.add(tfBroadcast, BorderLayout.CENTER);

        btnBroadcast = UITheme.createPrimaryButton("Отправить");
        btnBroadcast.addActionListener(e -> sendBroadcast());
        panel.add(btnBroadcast, BorderLayout.EAST);

        return panel;
    }

    /**
     * Стилизация таблиц в темной теме.
     */
    private void styleTable(JTable table) {
        table.setBackground(UITheme.BG_CARD);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER_SUBTLE);
        table.setFont(UITheme.FONT_REGULAR);
        table.setRowHeight(30);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        table.getTableHeader().setBackground(UITheme.BG_INPUT);
        table.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        table.getTableHeader().setFont(UITheme.FONT_HEADER);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(UITheme.BORDER_SUBTLE));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, isSelected, hasFocus, r, c);
                if (!isSelected) {
                    comp.setBackground(r % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_INPUT.darker());
                    comp.setForeground(UITheme.TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
    }

    // --- Действия пользователя и администратора ---

    private void startServer() {
        int port = (int) portSpinner.getValue();
        boolean ok = server.start(port);
        if (!ok) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось запустить сервер на порту " + port + "!\nУбедитесь, что порт не занят другим приложением.",
                    "Ошибка запуска", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        server.stop();
    }

    private void showCreateRoomDialog() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Сначала запустите сервер!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = UITheme.createTextField("например: laborator-retele");
        JTextField descField = UITheme.createTextField("например: Обсуждение лабораторной работы №3");

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.add(new JLabel("Название комнаты (префикс # добавится автоматически):"));
        form.add(nameField);
        form.add(new JLabel("Описание комнаты:"));
        form.add(descField);

        int res = JOptionPane.showConfirmDialog(this, form, "Создание новой комнаты чата",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Название комнаты не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean created = server.createRoom(name, desc, "Admin Server");
            if (!created) {
                JOptionPane.showMessageDialog(this, "Комната с таким названием уже существует!", "Внимание", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedRoom() {
        if (!server.isRunning()) return;
        int row = roomsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите комнату в таблице!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String roomName = (String) roomsTableModel.getValueAt(row, 0);
        if (roomName.equalsIgnoreCase("#general")) {
            JOptionPane.showMessageDialog(this, "Главная комната #general является обязательной и не может быть удалена!", "Запрещено", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить комнату " + roomName + "?\nВсе находящиеся в ней пользователи будут перенесены в #general.",
                "Подтверждение удаления", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            server.deleteRoom(roomName);
        }
    }

    private void kickSelectedUser() {
        if (!server.isRunning()) return;
        int row = clientsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите клиента в таблице!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = (String) clientsTableModel.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Укажите причину отключения:", "Отключение пользователя (Kick)", JOptionPane.QUESTION_MESSAGE);
        if (reason != null) {
            server.kickUser(username, reason);
        }
    }

    private void sendBroadcast() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Сначала запустите сервер!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String text = tfBroadcast.getText().trim();
        if (text.isEmpty()) return;

        server.broadcastServerAnnouncement(text);
        tfBroadcast.setText("");
    }

    private void exportLogs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("server_audit_" + System.currentTimeMillis() + ".log"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(chooser.getSelectedFile()))) {
                for (int i = 0; i < logsTableModel.getRowCount(); i++) {
                    pw.println("[" + logsTableModel.getValueAt(i, 0) + "] " +
                            logsTableModel.getValueAt(i, 1) + " | " +
                            logsTableModel.getValueAt(i, 2) + " -> " +
                            logsTableModel.getValueAt(i, 3));
                }
                JOptionPane.showMessageDialog(this, "Журнал аудита успешно экспортирован!", "Успешно", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при экспорте: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshRoomsTable() {
        SwingUtilities.invokeLater(() -> {
            roomsTableModel.setRowCount(0);
            List<ChatRoom> list = server.getRoomsList();
            for (ChatRoom r : list) {
                roomsTableModel.addRow(new Object[]{
                        r.getName(),
                        r.getDescription(),
                        r.getCreatedBy(),
                        r.getUserCount()
                });
            }
            lblRoomsVal.setText(String.valueOf(list.size()));
        });
    }

    private void refreshClientsTable() {
        SwingUtilities.invokeLater(() -> {
            clientsTableModel.setRowCount(0);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            List<ClientInfo> list = server.getConnectedClientsInfo();
            for (ClientInfo ci : list) {
                clientsTableModel.addRow(new Object[]{
                        ci.getUsername(),
                        ci.getIpAddress(),
                        ci.getPort(),
                        ci.getCurrentRoom(),
                        sdf.format(new Date(ci.getConnectedAt()))
                });
            }
            lblClientsVal.setText(String.valueOf(list.size()));
        });
    }

    private void refreshStats() {
        SwingUtilities.invokeLater(() -> {
            boolean active = server.isRunning();
            lblStateVal.setText(active ? "АКТИВЕН" : "ОСТАНОВЛЕН");
            lblStateVal.setForeground(active ? UITheme.SUCCESS : UITheme.DANGER);

            statusBadge.setText(active ? "● ОНЛАЙН: " + server.getPort() : "● ОСТАНОВЛЕН");
            statusBadge.setBackground(active ? UITheme.SUCCESS : UITheme.DANGER);

            btnStart.setEnabled(!active);
            btnStop.setEnabled(active);
            portSpinner.setEnabled(!active);
        });
    }

    // --- Реализация интерфейса ServerListener (обратные вызовы событий) ---

    @Override
    public void onServerStarted(int port, String localIp) {
        refreshStats();
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("START", "Сервер запущен на порту " + port + " (Локальный IP: " + localIp + ")");
    }

    @Override
    public void onServerStopped() {
        refreshStats();
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("STOP", "Сервер остановлен.");
    }

    @Override
    public void onClientConnected(ClientInfo client) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("ПОДКЛЮЧЕНИЕ", "Клиент " + client.getUsername() + " подключился с адреса " + client.getIpAddress());
    }

    @Override
    public void onClientDisconnected(ClientInfo client) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("ОТКЛЮЧЕНИЕ", "Клиент " + client.getUsername() + " отключился.");
    }

    @Override
    public void onClientRoomChanged(ClientInfo client, String oldRoom, String newRoom) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("КОМНАТА", client.getUsername() + " перешел из " + oldRoom + " в " + newRoom);
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        totalMessagesCount++;
        lblMessagesVal.setText(String.valueOf(totalMessagesCount));
        String details = message.getText();
        if (message.isReply()) {
            details = "[Ответ на @" + message.getReplyToAuthor() + "] " + details;
        }
        onLogEvent("ЧАТ", "[" + message.getTargetRoom() + "] " + message.getSender() + ": " + details);
    }

    @Override
    public void onFileTransferred(NetworkMessage message) {
        totalFilesCount++;
        lblFilesVal.setText(String.valueOf(totalFilesCount));
        onLogEvent("ФАЙЛ", "[" + message.getTargetRoom() + "] " + message.getSender() + " передал файл: " +
                message.getFileName() + " (" + message.getFormattedFileSize() + ")");
    }

    @Override
    public void onRoomCreated(ChatRoom room) {
        refreshRoomsTable();
        onLogEvent("КОМНАТА", "Создана новая комната: " + room.getName() + " (" + room.getDescription() + ")");
    }

    @Override
    public void onRoomDeleted(String roomName) {
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("КОМНАТА", "Комната " + roomName + " удалена.");
    }

    @Override
    public void onLogEvent(String level, String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            logsTableModel.insertRow(0, new Object[]{
                    sdf.format(new Date()),
                    level,
                    "Сервер",
                    message
            });
        });
    }
}
