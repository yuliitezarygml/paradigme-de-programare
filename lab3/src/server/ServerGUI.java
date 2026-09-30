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
 * Interfața Grafică (GUI) Modernă pentru Administrarea Serverului de Rețea Locală.
 * Oferă:
 * - Butoane de Start/Stop/Configurare port
 * - Indicatori statistici în timp real (Camere, Utilizatori, Mesaje, Fișiere)
 * - Managementul Camerelor (vizualizare, creare, ștergere)
 * - Monitorizarea Clienților conectați și posibilitatea de Kick
 * - Jurnal complet de evenimente și istoric global de mesaje cu export
 * - Trimitere de anunțuri globale (Broadcast)
 */
public class ServerGUI extends JFrame implements ServerListener {

    private final ChatServer server;

    // Controale de vârf
    private JLabel statusBadge;
    private JLabel ipBadge;
    private JSpinner portSpinner;
    private JButton btnStart;
    private JButton btnStop;

    // Carduri Statistice
    private JLabel lblStateVal;
    private JLabel lblRoomsVal;
    private JLabel lblClientsVal;
    private JLabel lblMessagesVal;
    private JLabel lblFilesVal;

    private int totalMessagesCount = 0;
    private int totalFilesCount = 0;

    // Tabele
    private DefaultTableModel roomsTableModel;
    private JTable roomsTable;

    private DefaultTableModel clientsTableModel;
    private JTable clientsTable;

    private DefaultTableModel logsTableModel;
    private JTable logsTable;

    // Câmp anunț global
    private JTextField tfBroadcast;
    private JButton btnBroadcast;

    public ServerGUI() {
        super("Server Chat Local - Panou de Control (Lab 3)");
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

        // 1. Header & Control Bar
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 2. Center Content (Stat cards + Tabs)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setBackground(UITheme.BG_DARKER);
        centerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Stat Cards
        JPanel statsPanel = createStatsCardsPanel();
        centerPanel.add(statsPanel, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabbedPane = createTabsPanel();
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Broadcast Bar
        JPanel bottomPanel = createBroadcastBar();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(UITheme.BG_SIDEBAR);
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));

        // Stânga: Titlu și Subtitlu
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("⚡ SERVER CHAT LOCAL (LAB 3)");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Monitorizare socket-uri, camere multiple, transfer fișiere și audit istoric");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        panel.add(titlePanel, BorderLayout.WEST);

        // Dreapta: Status badges, Port și butoane Start/Stop
        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ctrlPanel.setOpaque(false);

        String localIp = ChatServer.detectLocalIP();
        ipBadge = UITheme.createStatusBadge("IP: " + localIp, UITheme.BG_CARD, UITheme.TEXT_ACCENT);
        statusBadge = UITheme.createStatusBadge("● OPRIT", UITheme.DANGER, Color.WHITE);

        JLabel lblPort = new JLabel("Port:");
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

        btnStart = UITheme.createSuccessButton("▶ Pornire Server");
        btnStart.addActionListener(e -> startServer());

        btnStop = UITheme.createDangerButton("⏹ Oprire");
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

    private JPanel createStatsCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 12, 0));
        panel.setOpaque(false);

        lblStateVal = new JLabel("Oprit");
        lblRoomsVal = new JLabel("3");
        lblClientsVal = new JLabel("0");
        lblMessagesVal = new JLabel("0");
        lblFilesVal = new JLabel("0");

        panel.add(createCard("Stare Server", lblStateVal, UITheme.DANGER));
        panel.add(createCard("Camere Active", lblRoomsVal, UITheme.ACCENT));
        panel.add(createCard("Clienți Conectați", lblClientsVal, UITheme.SUCCESS));
        panel.add(createCard("Mesaje Tranzitate", lblMessagesVal, UITheme.WARNING));
        panel.add(createCard("Fișiere Trimise", lblFilesVal, UITheme.INFO));

        return panel;
    }

    private JPanel createCard(String title, JLabel valLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Bară subtilă colorată în stânga
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

    private JTabbedPane createTabsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(UITheme.BG_SIDEBAR);
        tabs.setForeground(UITheme.TEXT_PRIMARY);
        tabs.setFont(UITheme.FONT_HEADER);

        tabs.addTab("📁 Camere de Chat", createRoomsTab());
        tabs.addTab("👥 Clienți Conectați", createClientsTab());
        tabs.addTab("📜 Jurnal & Istoric Global", createLogsTab());

        return tabs;
    }

    private JPanel createRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Toolbar de sus pentru Camere
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnAddRoom = UITheme.createPrimaryButton("➕ Creare Cameră Nouă");
        btnAddRoom.addActionListener(e -> showCreateRoomDialog());

        JButton btnDeleteRoom = UITheme.createDangerButton("🗑️ Șterge Camera Selectată");
        btnDeleteRoom.addActionListener(e -> deleteSelectedRoom());

        bar.add(btnAddRoom);
        bar.add(btnDeleteRoom);
        panel.add(bar, BorderLayout.NORTH);

        // Tabel Camere
        String[] cols = {"Nume Cameră", "Descriere", "Creată De", "Utilizatori Activi"};
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

    private JPanel createClientsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnKick = UITheme.createDangerButton("⚡ Deconectează Utilizator (Kick)");
        btnKick.addActionListener(e -> kickSelectedUser());

        bar.add(btnKick);
        panel.add(bar, BorderLayout.NORTH);

        String[] cols = {"Nume Utilizator", "Adresă IP", "Port", "Cameră Curentă", "Conectat La"};
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

    private JPanel createLogsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG_CHAT);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);

        JButton btnExport = UITheme.createSecondaryButton("📥 Exportă Jurnal (.log)");
        btnExport.addActionListener(e -> exportLogs());

        JButton btnClear = UITheme.createSecondaryButton("🧹 Curăță Ecranul");
        btnClear.addActionListener(e -> logsTableModel.setRowCount(0));

        bar.add(btnExport);
        bar.add(btnClear);
        panel.add(bar, BorderLayout.NORTH);

        String[] cols = {"Timp", "Nivel / Tip", "Sursă / Client", "Mesaj / Detalii"};
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

    private JPanel createBroadcastBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(UITheme.BG_SIDEBAR);
        panel.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel lbl = new JLabel("📢 Anunț către toți utilizatorii:");
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(lbl, BorderLayout.WEST);

        tfBroadcast = UITheme.createTextField("Scrie un anunț de sistem care va apărea în toate camerele...");
        tfBroadcast.addActionListener(e -> sendBroadcast());
        panel.add(tfBroadcast, BorderLayout.CENTER);

        btnBroadcast = UITheme.createPrimaryButton("Trimite Anunț");
        btnBroadcast.addActionListener(e -> sendBroadcast());
        panel.add(btnBroadcast, BorderLayout.EAST);

        return panel;
    }

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

    // --- Acțiuni utilizator ---

    private void startServer() {
        int port = (int) portSpinner.getValue();
        boolean ok = server.start(port);
        if (!ok) {
            JOptionPane.showMessageDialog(this,
                    "Nu s-a putut porni serverul pe portul " + port + "!\nVerificați dacă portul nu este ocupat de altă aplicație.",
                    "Eroare Pornire", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        server.stop();
    }

    private void showCreateRoomDialog() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Porniți mai întâi serverul!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = UITheme.createTextField("ex: laborator-retele");
        JTextField descField = UITheme.createTextField("ex: Discuții despre laboratorul 3");

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 6));
        form.add(new JLabel("Numele camerei (va primi automat prefixul #):"));
        form.add(nameField);
        form.add(new JLabel("Descrierea camerei:"));
        form.add(descField);

        int res = JOptionPane.showConfirmDialog(this, form, "Creare Cameră Chat Nouă",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Numele camerei nu poate fi gol!", "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean created = server.createRoom(name, desc, "Admin Server");
            if (!created) {
                JOptionPane.showMessageDialog(this, "O cameră cu acest nume există deja!", "Atenție", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedRoom() {
        if (!server.isRunning()) return;
        int row = roomsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selectați o cameră din tabel!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String roomName = (String) roomsTableModel.getValueAt(row, 0);
        if (roomName.equalsIgnoreCase("#general")) {
            JOptionPane.showMessageDialog(this, "Camera principală #general nu poate fi ștearsă!", "Interzis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Sigur doriți să ștergeți camera " + roomName + "?\nToți utilizatorii din ea vor fi mutați în #general.",
                "Confirmare Ștergere", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            server.deleteRoom(roomName);
        }
    }

    private void kickSelectedUser() {
        if (!server.isRunning()) return;
        int row = clientsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selectați un client din tabel!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = (String) clientsTableModel.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Introduceți motivul pentru deconectare:", "Kick Client", JOptionPane.QUESTION_MESSAGE);
        if (reason != null) {
            server.kickUser(username, reason);
        }
    }

    private void sendBroadcast() {
        if (!server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Porniți mai întâi serverul!", "Atenție", JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Jurnalul a fost exportat cu succes!", "Succes", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la export: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
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
            lblStateVal.setText(active ? "ACTIV" : "OPRIT");
            lblStateVal.setForeground(active ? UITheme.SUCCESS : UITheme.DANGER);

            statusBadge.setText(active ? "● ONLINE: " + server.getPort() : "● OPRIT");
            statusBadge.setBackground(active ? UITheme.SUCCESS : UITheme.DANGER);

            btnStart.setEnabled(!active);
            btnStop.setEnabled(active);
            portSpinner.setEnabled(!active);
        });
    }

    // --- Implementare ServerListener ---

    @Override
    public void onServerStarted(int port, String localIp) {
        refreshStats();
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("START", "Serverul a pornit pe portul " + port + " (IP LAN: " + localIp + ")");
    }

    @Override
    public void onServerStopped() {
        refreshStats();
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("STOP", "Serverul s-a oprit.");
    }

    @Override
    public void onClientConnected(ClientInfo client) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("CONEXIUNE", "Clientul " + client.getUsername() + " s-a conectat de la " + client.getIpAddress());
    }

    @Override
    public void onClientDisconnected(ClientInfo client) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("DECONECTARE", "Clientul " + client.getUsername() + " s-a deconectat.");
    }

    @Override
    public void onClientRoomChanged(ClientInfo client, String oldRoom, String newRoom) {
        refreshClientsTable();
        refreshRoomsTable();
        onLogEvent("CAMERĂ", client.getUsername() + " a trecut din " + oldRoom + " în " + newRoom);
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        totalMessagesCount++;
        lblMessagesVal.setText(String.valueOf(totalMessagesCount));
        String details = message.getText();
        if (message.isReply()) {
            details = "[Răspuns la @" + message.getReplyToAuthor() + "] " + details;
        }
        onLogEvent("CHAT", "[" + message.getTargetRoom() + "] " + message.getSender() + ": " + details);
    }

    @Override
    public void onFileTransferred(NetworkMessage message) {
        totalFilesCount++;
        lblFilesVal.setText(String.valueOf(totalFilesCount));
        onLogEvent("FIȘIER", "[" + message.getTargetRoom() + "] " + message.getSender() + " a trimis fișierul: " +
                message.getFileName() + " (" + message.getFormattedFileSize() + ")");
    }

    @Override
    public void onRoomCreated(ChatRoom room) {
        refreshRoomsTable();
        onLogEvent("CAMERĂ", "Cameră nouă creată: " + room.getName() + " (" + room.getDescription() + ")");
    }

    @Override
    public void onRoomDeleted(String roomName) {
        refreshRoomsTable();
        refreshClientsTable();
        onLogEvent("CAMERĂ", "Camera " + roomName + " a fost ștearsă.");
    }

    @Override
    public void onLogEvent(String level, String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            logsTableModel.insertRow(0, new Object[]{
                    sdf.format(new Date()),
                    level,
                    "Server",
                    message
            });
        });
    }
}
