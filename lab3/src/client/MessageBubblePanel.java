package client;

import common.NetworkMessage;
import common.UITheme;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Componentă vizuală modernă pentru afișarea unui mesaj de chat.
 * Suportă:
 * - Avatar colorat cu inițiale
 * - Diferențiere mesaje proprii (Self) vs ceilalți utilizatori
 * - Casetă de citat pentru Răspuns (Reply)
 * - Card interactiv pentru transfer și descărcare fișiere
 * - Buton acțiune "Răspunde" (Reply)
 */
public class MessageBubblePanel extends JPanel {

    private final NetworkMessage message;
    private final boolean isSelf;
    private final Consumer<NetworkMessage> onReplyAction;
    private File savedFile;

    public MessageBubblePanel(NetworkMessage message, String currentUsername, Consumer<NetworkMessage> onReplyAction) {
        this.message = message;
        this.isSelf = message.getSender() != null && message.getSender().equalsIgnoreCase(currentUsername);
        this.onReplyAction = onReplyAction;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 12, 6, 12));

        if ("SISTEM".equalsIgnoreCase(message.getSender())) {
            buildSystemNotification();
        } else {
            buildStandardBubble();
        }
    }

    /**
     * Construiește o notificare de sistem centrată (pastilă / pill).
     */
    private void buildSystemNotification() {
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrapper.setOpaque(false);

        JLabel lbl = new JLabel(message.getText());
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);

        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(UITheme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(4, 12, 4, 12));
        pill.add(lbl);

        centerWrapper.add(pill);
        add(centerWrapper, BorderLayout.CENTER);
    }

    /**
     * Construiește bula modernă de mesaj cu avatar, antet, citat reply și conținut.
     */
    private void buildStandardBubble() {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setOpaque(false);

        // 1. Avatar rotund în stânga
        JPanel avatarPanel = createAvatarPanel(message.getSender());
        rowPanel.add(avatarPanel, BorderLayout.WEST);

        // 2. Conținutul bulei (Header + Reply Quote + Text/File)
        JPanel bubbleBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(isSelf ? UITheme.BUBBLE_SELF : UITheme.BUBBLE_OTHER);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(UITheme.BUBBLE_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        bubbleBox.setOpaque(false);
        bubbleBox.setLayout(new BoxLayout(bubbleBox, BoxLayout.Y_AXIS));
        bubbleBox.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Antet: Nume + Timp + Buton Reply
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);

        JLabel lblSender = new JLabel(message.getSender() + (isSelf ? " (Tu)" : ""));
        lblSender.setFont(UITheme.FONT_BOLD);
        lblSender.setForeground(isSelf ? UITheme.ACCENT_LIGHT : UITheme.TEXT_PRIMARY);

        JLabel lblTime = new JLabel(message.getFormattedTime());
        lblTime.setFont(UITheme.FONT_SMALL);
        lblTime.setForeground(UITheme.TEXT_MUTED);

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftHeader.setOpaque(false);
        leftHeader.add(lblSender);
        leftHeader.add(lblTime);
        headerPanel.add(leftHeader, BorderLayout.WEST);

        // Buton discret "↩ Răspunde"
        JLabel btnReply = new JLabel("↩ Răspunde");
        btnReply.setFont(UITheme.FONT_TINY);
        btnReply.setForeground(UITheme.TEXT_MUTED);
        btnReply.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReply.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnReply.setForeground(UITheme.ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnReply.setForeground(UITheme.TEXT_MUTED);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onReplyAction != null) {
                    onReplyAction.accept(message);
                }
            }
        });
        headerPanel.add(btnReply, BorderLayout.EAST);
        bubbleBox.add(headerPanel);

        // Dacă mesajul este un răspuns (Reply) la alt mesaj, afișăm citatul!
        if (message.isReply()) {
            bubbleBox.add(Box.createVerticalStrut(6));
            bubbleBox.add(createQuoteBox());
        }

        bubbleBox.add(Box.createVerticalStrut(6));

        // Conținut: Mesaj Text sau Card de Fișier
        if (message.isFile()) {
            bubbleBox.add(createFileCard());
        } else {
            JTextArea textArea = new JTextArea(message.getText());
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setFont(UITheme.FONT_REGULAR);
            textArea.setForeground(UITheme.TEXT_PRIMARY);
            textArea.setBorder(null);
            bubbleBox.add(textArea);
        }

        rowPanel.add(bubbleBox, BorderLayout.CENTER);
        add(rowPanel, BorderLayout.CENTER);
    }

    /**
     * Creează un cerc de avatar cu inițiale colorat distinctiv.
     */
    private JPanel createAvatarPanel(String sender) {
        Color color = UITheme.getAvatarColor(sender);
        String initials = UITheme.getInitials(sender);

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                g2.fillOval(0, 0, 36, 36);

                g2.setColor(Color.WHITE);
                g2.setFont(UITheme.FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                int x = (36 - fm.stringWidth(initials)) / 2;
                int y = ((36 - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initials, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(36, 36);
            }
        };
        avatar.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(avatar, BorderLayout.NORTH);
        return wrapper;
    }

    /**
     * Creează căsuța de citare pentru mesajul original la care s-a răspuns.
     */
    private JPanel createQuoteBox() {
        JPanel quote = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.QUOTE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Bară verticală de accent
                g2.setColor(UITheme.ACCENT);
                g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
                g2.dispose();
            }
        };
        quote.setOpaque(false);
        quote.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel lblQuote = new JLabel("↳ @" + message.getReplyToAuthor() + ": \"" + message.getReplyToSnippet() + "\"");
        lblQuote.setFont(UITheme.FONT_SMALL);
        lblQuote.setForeground(UITheme.TEXT_MUTED);
        quote.add(lblQuote, BorderLayout.CENTER);

        return quote;
    }

    /**
     * Construiește cardul interactiv pentru transferul și descărcarea de fișiere.
     */
    private JPanel createFileCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                UITheme.enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UITheme.FILE_CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(UITheme.BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Iconiță fișier
        JLabel lblIcon = new JLabel("📁");
        lblIcon.setFont(new Font("SansSerif", Font.PLAIN, 28));
        card.add(lblIcon, BorderLayout.WEST);

        // Info fișier (nume + mărime)
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
        info.setOpaque(false);

        JLabel lblName = new JLabel(message.getFileName());
        lblName.setFont(UITheme.FONT_BOLD);
        lblName.setForeground(UITheme.TEXT_PRIMARY);

        JLabel lblSize = new JLabel(message.getFormattedFileSize());
        lblSize.setFont(UITheme.FONT_SMALL);
        lblSize.setForeground(UITheme.TEXT_MUTED);

        info.add(lblName);
        info.add(lblSize);
        card.add(info, BorderLayout.CENTER);

        // Butoane Salvare / Deschidere
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        JButton btnSave = UITheme.createPrimaryButton("⬇ Descarcă");
        JButton btnOpen = UITheme.createSecondaryButton("📂 Deschide");
        btnOpen.setVisible(false);

        btnSave.addActionListener(e -> {
            saveFileAction(btnSave, btnOpen);
        });

        btnOpen.addActionListener(e -> {
            if (savedFile != null && savedFile.exists()) {
                try {
                    Desktop.getDesktop().open(savedFile);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Nu s-a putut deschide fișierul: " + ex.getMessage(),
                            "Eroare", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        actions.add(btnSave);
        actions.add(btnOpen);
        card.add(actions, BorderLayout.EAST);

        return card;
    }

    private void saveFileAction(JButton btnSave, JButton btnOpen) {
        if (message.getFileData() == null || message.getFileData().length == 0) {
            JOptionPane.showMessageDialog(this, "Fișierul nu conține date!", "Eroare", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(message.getFileName()));
        int choice = chooser.showSaveDialog(this);

        if (choice == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(message.getFileData());
                savedFile = dest;
                btnSave.setText("✅ Salvat");
                btnSave.setEnabled(false);
                btnOpen.setVisible(true);
                revalidate();
                repaint();
                JOptionPane.showMessageDialog(this, "Fișier salvat cu succes în:\n" + dest.getAbsolutePath(),
                        "Descărcare Reușită", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la salvare: " + ex.getMessage(),
                        "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
