package client;

import common.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Interfața Grafică (GUI) Modernă a Clientului de Chat.
 * Implementează toate cerințele din laboratorul 3:
 * - a. Transmiterea și recepționarea mesajelor în rețea
 * - b. Afișarea istoriei mesajelor primite și căutare
 * - c. Răspuns la mesaj recepționat (Reply) cu bară vizuală de citare
 * - d. Transmiterea și descărcarea fișierelor
 * - e. Crearea și navigarea între Camere de Chat (Chat-rooms)
 */
public class ClientGUI extends JFrame implements ClientListener {

    private final ChatClient client;
    private final List<NetworkMessage> receivedHistory = new ArrayList<>();
    private final List<ChatRoom> currentRooms = new ArrayList<>();

    // Conexiune
    private JTextField tfHost;
    private JTextField tfPort;
    private JTextField tfUsername;
    private JButton btnConnect;
    private JLabel statusBadge;

    // Sidebar
    private JPanel profilePanel;
    private JLabel lblUserInitials;
    private JLabel lblUsername;
    private DefaultListModel<ChatRoom> roomsListModel;
    private JList<ChatRoom> roomsList;
    private DefaultListModel<String> usersListModel;
    private JList<String> usersList;

    // Chat principal
    private JLabel lblChatTitle;
    private JLabel lblChatSubtitle;
    private JPanel messagesContainer;
    private JScrollPane chatScrollPane;

    // Panou Citat Răspuns (Reply Bar)
    private JPanel replyBanner;
    private JLabel lblReplyInfo;
    private JButton btnCancelReply;
    private NetworkMessage activeReplyTarget = null;

    // Câmp de intrare
    private JTextField tfInput;
    private JButton btnSend;
    private JButton btnSendFile;
    private JButton btnHistory;

    public ClientGUI() {
        super("Chat Rețea Locală (Lab 3)");
        this.client = new ChatClient();
        this.client.setListener(this);

        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DARKER);
        setLayout(new BorderLayout());

        // 1. Bara de conectare de sus
        add(createConnectionBar(), BorderLayout.NORTH);

        // 2. Corpul Principal: Sidebar (Stânga) + Chat (Centru)
        JPanel mainBody = new JPanel(new BorderLayout());
        mainBody.setBackground(UITheme.BG_DARKER);

        mainBody.add(createSidebar(), BorderLayout.WEST);
        mainBody.add(createChatArea(), BorderLayout.CENTER);

        add(mainBody, BorderLayout.CENTER);

        // Închidere sigură la ieșire
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client.isConnected()) {
                    client.disconnect("Clientul a închis aplicația.");
                }
            }
        });

        updateConnectedState(false);
    }

    private JPanel createConnectionBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(UITheme.BG_SIDEBAR);
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        // Stânga: Logo & Titlu
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel lblLogo = new JLabel("💬");
        lblLogo.setFont(new Font("SansSerif", Font.PLAIN, 20));
        JLabel lblTitle = new JLabel("CHAT LOCAL LAN");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        left.add(lblLogo);
        left.add(lblTitle);
        bar.add(left, BorderLayout.WEST);

        // Dreapta: Setări conectare
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel lblH = new JLabel("Host:");
        lblH.setFont(UITheme.FONT_BOLD);
        lblH.setForeground(UITheme.TEXT_MUTED);

        tfHost = UITheme.createTextField("IP Server");
        tfHost.setText("127.0.0.1");
        tfHost.setPreferredSize(new Dimension(110, 32));

        JLabel lblP = new JLabel("Port:");
        lblP.setFont(UITheme.FONT_BOLD);
        lblP.setForeground(UITheme.TEXT_MUTED);

        tfPort = UITheme.createTextField("Port");
        tfPort.setText("8888");
        tfPort.setPreferredSize(new Dimension(65, 32));

        JLabel lblU = new JLabel("Nume:");
        lblU.setFont(UITheme.FONT_BOLD);
        lblU.setForeground(UITheme.TEXT_MUTED);

        tfUsername = UITheme.createTextField("Numele tău");
        tfUsername.setText("Student_" + (int)(Math.random() * 900 + 100));
        tfUsername.setPreferredSize(new Dimension(130, 32));

        statusBadge = UITheme.createStatusBadge("● DECONECTAT", UITheme.DANGER, Color.WHITE);

        btnConnect = UITheme.createPrimaryButton("Conectare");
        btnConnect.addActionListener(e -> toggleConnection());

        right.add(statusBadge);
        right.add(lblH);
        right.add(tfHost);
        right.add(lblP);
        right.add(tfPort);
        right.add(lblU);
        right.add(tfUsername);
        right.add(btnConnect);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(new EmptyBorder(12, 10, 12, 10));

        // Panou profil utilizator
        profilePanel = new JPanel(new BorderLayout(10, 0));
        profilePanel.setBackground(UITheme.BG_CARD);
        profilePanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        lblUserInitials = new JLabel("??", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblUserInitials.setPreferredSize(new Dimension(32, 32));
        lblUserInitials.setFont(UITheme.FONT_BOLD);
        lblUserInitials.setForeground(Color.WHITE);

        lblUsername = new JLabel("Deconectat");
        lblUsername.setFont(UITheme.FONT_BOLD);
        lblUsername.setForeground(UITheme.TEXT_PRIMARY);

        profilePanel.add(lblUserInitials, BorderLayout.WEST);
        profilePanel.add(lblUsername, BorderLayout.CENTER);
        sidebar.add(profilePanel, BorderLayout.NORTH);

        // Centru: Camere și Utilizatori
        JPanel listsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        listsPanel.setOpaque(false);

        // Secțiunea Camere
        JPanel roomsSection = new JPanel(new BorderLayout(0, 6));
        roomsSection.setOpaque(false);

        JPanel roomsHeader = new JPanel(new BorderLayout());
        roomsHeader.setOpaque(false);
        JLabel lblRooms = new JLabel("CAMERE CHAT");
        lblRooms.setFont(UITheme.FONT_TINY);
        lblRooms.setForeground(UITheme.TEXT_MUTED);

        JButton btnAddRoom = new JButton("➕") {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                super.paintComponent(g);
            }
        };
        btnAddRoom.setToolTipText("Creează o cameră nouă de discuție");
        btnAddRoom.setFont(UITheme.FONT_BOLD);
        btnAddRoom.setForeground(UITheme.TEXT_PRIMARY);
        btnAddRoom.setBackground(UITheme.BG_INPUT);
        btnAddRoom.setBorder(new EmptyBorder(2, 6, 2, 6));
        btnAddRoom.setContentAreaFilled(false);
        btnAddRoom.setFocusPainted(false);
        btnAddRoom.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddRoom.addActionListener(e -> promptCreateRoom());

        roomsHeader.add(lblRooms, BorderLayout.WEST);
        roomsHeader.add(btnAddRoom, BorderLayout.EAST);
        roomsSection.add(roomsHeader, BorderLayout.NORTH);

        roomsListModel = new DefaultListModel<>();
        roomsList = new JList<>(roomsListModel);
        styleList(roomsList);
        roomsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatRoom sel = roomsList.getSelectedValue();
                if (sel != null && !sel.getName().equalsIgnoreCase(client.getCurrentRoom())) {
                    client.joinRoom(sel.getName());
                }
            }
        });

        JScrollPane roomsScroll = new JScrollPane(roomsList);
        UITheme.applyModernScrollBar(roomsScroll);
        roomsSection.add(roomsScroll, BorderLayout.CENTER);
        listsPanel.add(roomsSection);

        // Secțiunea Utilizatori Online
        JPanel usersSection = new JPanel(new BorderLayout(0, 6));
        usersSection.setOpaque(false);

        JLabel lblUsers = new JLabel("MEMBRI ÎN CAMERĂ");
        lblUsers.setFont(UITheme.FONT_TINY);
        lblUsers.setForeground(UITheme.TEXT_MUTED);
        usersSection.add(lblUsers, BorderLayout.NORTH);

        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        styleList(usersList);

        JScrollPane usersScroll = new JScrollPane(usersList);
        UITheme.applyModernScrollBar(usersScroll);
        usersSection.add(usersScroll, BorderLayout.CENTER);
        listsPanel.add(usersSection);

        sidebar.add(listsPanel, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel createChatArea() {
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(UITheme.BG_CHAT);

        // Antet Chat (Nume cameră, Descriere, Buton Istoric)
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(UITheme.BG_SIDEBAR);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel headerLeft = new JPanel(new GridLayout(2, 1, 0, 2));
        headerLeft.setOpaque(false);

        lblChatTitle = new JLabel("#general");
        lblChatTitle.setFont(UITheme.FONT_TITLE);
        lblChatTitle.setForeground(UITheme.TEXT_PRIMARY);

        lblChatSubtitle = new JLabel("Camera principală de discuție");
        lblChatSubtitle.setFont(UITheme.FONT_SMALL);
        lblChatSubtitle.setForeground(UITheme.TEXT_MUTED);

        headerLeft.add(lblChatTitle);
        headerLeft.add(lblChatSubtitle);
        header.add(headerLeft, BorderLayout.WEST);

        // Buton Istoric în antet (Cerința b)
        btnHistory = UITheme.createSecondaryButton("📜 Istoric Mesaje");
        btnHistory.setToolTipText("Deschide și caută în istoricul mesajelor primite");
        btnHistory.addActionListener(e -> openHistoryDialog());
        header.add(btnHistory, BorderLayout.EAST);

        chatPanel.add(header, BorderLayout.NORTH);

        // Zonă de mesaje cu bule
        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(UITheme.BG_CHAT);
        messagesContainer.setBorder(new EmptyBorder(8, 0, 8, 0));

        chatScrollPane = new JScrollPane(messagesContainer);
        UITheme.applyModernScrollBar(chatScrollPane);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Partea inferioară: Bară de Reply + Bară de Intrare
        JPanel bottomArea = new JPanel(new BorderLayout());
        bottomArea.setOpaque(false);

        // Bară Reply (inițial ascunsă)
        replyBanner = createReplyBanner();
        bottomArea.add(replyBanner, BorderLayout.NORTH);

        // Bară de introducere mesaj
        JPanel inputBar = createInputBar();
        bottomArea.add(inputBar, BorderLayout.SOUTH);

        chatPanel.add(bottomArea, BorderLayout.SOUTH);
        return chatPanel;
    }

    /**
     * Construiește bara de previzualizare a răspunsului (Reply Banner).
     */
    private JPanel createReplyBanner() {
        JPanel banner = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UITheme.ACCENT);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        banner.setBorder(new EmptyBorder(6, 14, 6, 14));
        banner.setVisible(false);

        lblReplyInfo = new JLabel("↳ Răspuns către @User: \"...\"");
        lblReplyInfo.setFont(UITheme.FONT_SMALL);
        lblReplyInfo.setForeground(UITheme.TEXT_PRIMARY);
        banner.add(lblReplyInfo, BorderLayout.CENTER);

        btnCancelReply = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                super.paintComponent(g);
            }
        };
        btnCancelReply.setToolTipText("Anulează răspunsul");
        btnCancelReply.setFont(UITheme.FONT_BOLD);
        btnCancelReply.setForeground(UITheme.TEXT_MUTED);
        btnCancelReply.setContentAreaFilled(false);
        btnCancelReply.setBorderPainted(false);
        btnCancelReply.setFocusPainted(false);
        btnCancelReply.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelReply.addActionListener(e -> cancelReply());
        banner.add(btnCancelReply, BorderLayout.EAST);

        return banner;
    }

    private JPanel createInputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(UITheme.BG_SIDEBAR);
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        btnSendFile = UITheme.createSecondaryButton("📎 Fișier");
        btnSendFile.setToolTipText("Trimite un fișier în camera curentă");
        btnSendFile.addActionListener(e -> chooseAndSendFile());

        tfInput = UITheme.createTextField("Scrie un mesaj aici... (Enter pentru a trimite)");
        tfInput.addActionListener(e -> sendMessage());

        btnSend = UITheme.createPrimaryButton("Trimite ✈");
        btnSend.addActionListener(e -> sendMessage());

        bar.add(btnSendFile, BorderLayout.WEST);
        bar.add(tfInput, BorderLayout.CENTER);
        bar.add(btnSend, BorderLayout.EAST);

        return bar;
    }

    private <T> void styleList(JList<T> list) {
        list.setBackground(UITheme.BG_SIDEBAR);
        list.setForeground(UITheme.TEXT_PRIMARY);
        list.setFont(UITheme.FONT_REGULAR);
        list.setSelectionBackground(UITheme.BG_CARD);
        list.setSelectionForeground(UITheme.TEXT_PRIMARY);
        list.setFixedCellHeight(32);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object val, int idx, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(l, val, idx, isSelected, cellHasFocus);
                lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
                if (val instanceof ChatRoom) {
                    ChatRoom r = (ChatRoom) val;
                    lbl.setText(r.getName() + " (" + r.getUserCount() + ")");
                    if (r.getName().equalsIgnoreCase(client.getCurrentRoom())) {
                        lbl.setFont(UITheme.FONT_BOLD);
                        lbl.setForeground(UITheme.ACCENT_LIGHT);
                    } else {
                        lbl.setForeground(UITheme.TEXT_PRIMARY);
                    }
                } else if (val instanceof String) {
                    lbl.setText("🟢 " + val);
                    lbl.setForeground(UITheme.TEXT_PRIMARY);
                }
                return lbl;
            }
        });
    }

    // --- Operațiuni Utilizator ---

    private void toggleConnection() {
        if (client.isConnected()) {
            client.disconnect("Deconectat de utilizator.");
        } else {
            String host = tfHost.getText().trim();
            int port;
            try {
                port = Integer.parseInt(tfPort.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Portul trebuie să fie un număr valid!", "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String user = tfUsername.getText().trim();
            if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Introduceți un nume de utilizator!", "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnConnect.setEnabled(false);
            btnConnect.setText("Se conectează...");
            client.connect(host, port, user);
        }
    }

    private void sendMessage() {
        String text = tfInput.getText().trim();
        if (text.isEmpty() || !client.isConnected()) return;

        String repId = activeReplyTarget != null ? activeReplyTarget.getId() : null;
        String repAuthor = activeReplyTarget != null ? activeReplyTarget.getSender() : null;
        String repSnippet = activeReplyTarget != null ? getSnippet(activeReplyTarget) : null;

        client.sendChatMessage(text, repId, repAuthor, repSnippet);
        tfInput.setText("");
        cancelReply();
    }

    private void chooseAndSendFile() {
        if (!client.isConnected()) return;
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected == null || !selected.exists()) return;

            String repId = activeReplyTarget != null ? activeReplyTarget.getId() : null;
            String repAuthor = activeReplyTarget != null ? activeReplyTarget.getSender() : null;
            String repSnippet = activeReplyTarget != null ? getSnippet(activeReplyTarget) : null;

            boolean sent = client.sendFile(selected, repId, repAuthor, repSnippet);
            if (sent) {
                cancelReply();
            }
        }
    }

    private void promptCreateRoom() {
        if (!client.isConnected()) return;

        JTextField nameField = UITheme.createTextField("ex: laborator3-grupa");
        JTextField descField = UITheme.createTextField("ex: Camera de lucru pentru laborator");

        JPanel form = new JPanel(new GridLayout(4, 1, 0, 4));
        form.add(new JLabel("Numele camerei (va începe cu #):"));
        form.add(nameField);
        form.add(new JLabel("Descriere:"));
        form.add(descField);

        int opt = JOptionPane.showConfirmDialog(this, form, "Creare Chat-Room Nou", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (!name.isEmpty()) {
                client.createRoom(name, desc);
            }
        }
    }

    /**
     * Declanșează starea de Răspuns (Reply) pentru mesajul selectat.
     */
    public void startReply(NetworkMessage targetMsg) {
        this.activeReplyTarget = targetMsg;
        lblReplyInfo.setText("↳ Răspuns către @" + targetMsg.getSender() + ": \"" + getSnippet(targetMsg) + "\"");
        replyBanner.setVisible(true);
        tfInput.requestFocusInWindow();
        revalidate();
        repaint();
    }

    private void cancelReply() {
        this.activeReplyTarget = null;
        replyBanner.setVisible(false);
        revalidate();
        repaint();
    }

    private String getSnippet(NetworkMessage msg) {
        if (msg.isFile()) {
            return "Fișier: " + msg.getFileName();
        }
        String t = msg.getText();
        if (t == null) return "";
        return t.length() > 40 ? t.substring(0, 37) + "..." : t;
    }

    private void openHistoryDialog() {
        HistoryDialog dlg = new HistoryDialog(this, client.getCurrentRoom(), receivedHistory);
        dlg.setVisible(true);
    }

    private void appendBubble(NetworkMessage msg) {
        SwingUtilities.invokeLater(() -> {
            MessageBubblePanel bubble = new MessageBubblePanel(msg, client.getUsername(), this::startReply);
            messagesContainer.add(bubble);
            messagesContainer.revalidate();
            messagesContainer.repaint();
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vBar = chatScrollPane.getVerticalScrollBar();
            vBar.setValue(vBar.getMaximum());
        });
    }

    private void updateConnectedState(boolean connected) {
        btnConnect.setEnabled(true);
        btnConnect.setText(connected ? "Deconectare" : "Conectare");
        btnConnect.setBackground(connected ? UITheme.DANGER : UITheme.ACCENT);

        statusBadge.setText(connected ? "● ONLINE" : "● DECONECTAT");
        statusBadge.setBackground(connected ? UITheme.SUCCESS : UITheme.DANGER);

        tfHost.setEnabled(!connected);
        tfPort.setEnabled(!connected);
        tfUsername.setEnabled(!connected);

        tfInput.setEnabled(connected);
        btnSend.setEnabled(connected);
        btnSendFile.setEnabled(connected);
        btnHistory.setEnabled(connected);

        if (!connected) {
            lblUsername.setText("Deconectat");
            lblUserInitials.setText("??");
            roomsListModel.clear();
            usersListModel.clear();
        } else {
            lblUsername.setText(client.getUsername());
            lblUserInitials.setText(UITheme.getInitials(client.getUsername()));
        }
    }

    // --- Implementare ClientListener ---

    @Override
    public void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory) {
        SwingUtilities.invokeLater(() -> {
            updateConnectedState(true);
            onRoomListUpdated(rooms);
            onUserListUpdated(roomUsers);
            onRoomHistoryLoaded(client.getCurrentRoom(), initialHistory);
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            updateConnectedState(false);
            JOptionPane.showMessageDialog(this, error, "Eroare Conectare", JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            updateConnectedState(false);
            if (reason != null && !reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, reason, "Deconectat", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    @Override
    public void onMessageReceived(NetworkMessage message) {
        receivedHistory.add(message);
        if (message.getTargetRoom() != null && message.getTargetRoom().equalsIgnoreCase(client.getCurrentRoom())) {
            appendBubble(message);
        }
    }

    @Override
    public void onFileReceived(NetworkMessage message) {
        receivedHistory.add(message);
        if (message.getTargetRoom() != null && message.getTargetRoom().equalsIgnoreCase(client.getCurrentRoom())) {
            appendBubble(message);
        }
    }

    @Override
    public void onRoomListUpdated(List<ChatRoom> rooms) {
        SwingUtilities.invokeLater(() -> {
            currentRooms.clear();
            if (rooms != null) currentRooms.addAll(rooms);

            roomsListModel.clear();
            for (ChatRoom r : currentRooms) {
                roomsListModel.addElement(r);
                if (r.getName().equalsIgnoreCase(client.getCurrentRoom())) {
                    roomsList.setSelectedValue(r, true);
                    lblChatTitle.setText(r.getName());
                    lblChatSubtitle.setText(r.getDescription());
                }
            }
        });
    }

    @Override
    public void onUserListUpdated(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            usersListModel.clear();
            if (users != null) {
                for (String u : users) {
                    usersListModel.addElement(u);
                }
            }
        });
    }

    @Override
    public void onRoomHistoryLoaded(String room, List<NetworkMessage> history) {
        SwingUtilities.invokeLater(() -> {
            lblChatTitle.setText(room);
            messagesContainer.removeAll();

            if (history != null) {
                for (NetworkMessage m : history) {
                    if (!receivedHistory.contains(m)) {
                        receivedHistory.add(m);
                    }
                    MessageBubblePanel bubble = new MessageBubblePanel(m, client.getUsername(), this::startReply);
                    messagesContainer.add(bubble);
                }
            }
            messagesContainer.revalidate();
            messagesContainer.repaint();
            scrollToBottom();
        });
    }

    @Override
    public void onNotification(String text) {
        NetworkMessage sysMsg = NetworkMessage.createNotification(client.getCurrentRoom(), text);
        appendBubble(sysMsg);
    }

    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, error, "Atenție", JOptionPane.WARNING_MESSAGE);
        });
    }
}
