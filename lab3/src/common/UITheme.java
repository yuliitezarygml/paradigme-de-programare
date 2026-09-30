package common;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Gestionar centralizat pentru design-ul modern al interfeței grafice (UI).
 * Oferă o paletă elegantă Dark/Slate (inspirată din aplicații moderne precum Discord / Slack / Telegram),
 * butoane rotunjite, bare de defilare personalizate și randare antialiasing fină.
 */
public class UITheme {

    // --- Paleta de Culori (Modern Dark / Slate) ---
    public static final Color BG_DARKER      = new Color(0x11, 0x12, 0x14); // Bara superioară sau cel mai întunecat fundal
    public static final Color BG_SIDEBAR     = new Color(0x1E, 0x1F, 0x22); // Sidebar camere și utilizatori
    public static final Color BG_CHAT        = new Color(0x2B, 0x2D, 0x31); // Zona de mesaje
    public static final Color BG_CARD        = new Color(0x23, 0x24, 0x28); // Panouri, carduri, item-uri listă
    public static final Color BG_INPUT       = new Color(0x38, 0x3A, 0x40); // Câmpuri de text
    public static final Color BG_INPUT_FOCUS = new Color(0x40, 0x42, 0x49); // Câmpuri de text la focus
    public static final Color BORDER_SUBTLE  = new Color(0x3F, 0x41, 0x47); // Linii despărțitoare subtile

    // Accente și culori de stare
    public static final Color ACCENT         = new Color(0x58, 0x65, 0xF2); // Blurple / Indigo modern
    public static final Color ACCENT_HOVER   = new Color(0x47, 0x52, 0xC4);
    public static final Color ACCENT_LIGHT   = new Color(0x79, 0x83, 0xF5);
    public static final Color SUCCESS        = new Color(0x23, 0xA5, 0x5A); // Verde activ / online
    public static final Color SUCCESS_HOVER  = new Color(0x1C, 0x8B, 0x4B);
    public static final Color DANGER         = new Color(0xF2, 0x3F, 0x43); // Roșu deconectare / ștergere
    public static final Color DANGER_HOVER   = new Color(0xD8, 0x30, 0x34);
    public static final Color WARNING        = new Color(0xFA, 0xA6, 0x1A); // Galben atenționare
    public static final Color INFO           = new Color(0x00, 0xA8, 0xFC); // Albastru info

    // Bule de mesaje
    public static final Color BUBBLE_SELF    = new Color(0x3B, 0x42, 0x6A); // Mesaje proprii (indigo discret)
    public static final Color BUBBLE_OTHER   = new Color(0x31, 0x33, 0x38); // Mesaje de la ceilalți
    public static final Color BUBBLE_BORDER  = new Color(0x40, 0x42, 0x49);
    public static final Color QUOTE_BG       = new Color(0x1E, 0x1F, 0x22); // Citat reply
    public static final Color FILE_CARD_BG   = new Color(0x20, 0x22, 0x25); // Card transfer fișier

    // Tipografie și text
    public static final Color TEXT_PRIMARY   = new Color(0xF2, 0xF3, 0xF5); // Text principal alb discret
    public static final Color TEXT_MUTED     = new Color(0x94, 0x9B, 0xA4); // Text secundar / timestamp
    public static final Color TEXT_ACCENT    = new Color(0xDB, 0xDE, 0xE1); // Text etichete

    // Fonturi optimizate pentru ecran
    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_HEADER  = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD    = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_TINY    = new Font("SansSerif", Font.BOLD, 10);

    // Culori avatar plăcute
    private static final Color[] AVATAR_COLORS = {
            new Color(0xE9, 0x1E, 0x63),
            new Color(0x9C, 0x27, 0xB0),
            new Color(0x67, 0x3A, 0xB7),
            new Color(0x3F, 0x51, 0xB5),
            new Color(0x21, 0x96, 0xF3),
            new Color(0x00, 0x96, 0x88),
            new Color(0x4C, 0xAF, 0x50),
            new Color(0xFF, 0x98, 0x00),
            new Color(0x79, 0x55, 0x48)
    };

    /**
     * Activează antialiasing pentru randare fluidă de text și forme geometrice.
     */
    public static void enableAntiAliasing(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    /**
     * Obține culoarea avatarului determinist după numele de utilizator.
     */
    public static Color getAvatarColor(String username) {
        if (username == null || username.isEmpty()) return AVATAR_COLORS[0];
        int hash = Math.abs(username.hashCode());
        return AVATAR_COLORS[hash % AVATAR_COLORS.length];
    }

    /**
     * Extrage inițialele numelui de utilizator (ex: "Alex Popescu" -> "AP", "User" -> "US").
     */
    public static String getInitials(String username) {
        if (username == null || username.trim().isEmpty()) return "??";
        String clean = username.trim().replace("@", "").replace("#", "");
        String[] parts = clean.split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase();
        }
        return clean.toUpperCase();
    }

    /**
     * Creează un buton modern rotunjit cu efecte hover.
     */
    public static JButton createButton(String text, Color bg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Color currentBg = getModel().isPressed() ? hoverBg.darker() :
                                  (getModel().isRollover() ? hoverBg : bg);
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        return btn;
    }

    /**
     * Creează un buton primar accentuat (Indigo/Blurple).
     */
    public static JButton createPrimaryButton(String text) {
        return createButton(text, ACCENT, ACCENT_HOVER, TEXT_PRIMARY);
    }

    /**
     * Creează un buton de succes (Verde).
     */
    public static JButton createSuccessButton(String text) {
        return createButton(text, SUCCESS, SUCCESS_HOVER, TEXT_PRIMARY);
    }

    /**
     * Creează un buton de pericol (Roșu).
     */
    public static JButton createDangerButton(String text) {
        return createButton(text, DANGER, DANGER_HOVER, TEXT_PRIMARY);
    }

    /**
     * Creează un buton secundar discret (Gri).
     */
    public static JButton createSecondaryButton(String text) {
        return createButton(text, BG_INPUT, BG_INPUT_FOCUS, TEXT_PRIMARY);
    }

    /**
     * Configurează un JTextField cu design modern întunecat și margini rotunjite.
     */
    public static JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(hasFocus() ? BG_INPUT_FOCUS : BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(hasFocus() ? ACCENT : BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);

                if (getText().isEmpty() && !hasFocus() && placeholder != null) {
                    Graphics2D gPlaceholder = (Graphics2D) g.create();
                    enableAntiAliasing(gPlaceholder);
                    gPlaceholder.setColor(TEXT_MUTED);
                    gPlaceholder.setFont(getFont());
                    Insets insets = getInsets();
                    gPlaceholder.drawString(placeholder, insets.left, getHeight() / 2 + getFont().getSize() / 2 - 2);
                    gPlaceholder.dispose();
                }
            }
        };
        tf.setOpaque(false);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setFont(FONT_REGULAR);
        tf.setBorder(new EmptyBorder(8, 12, 8, 12));
        return tf;
    }

    /**
     * Creează un badge/pastilă de stare (pill) cu text și fundal colorat.
     */
    public static JLabel createStatusBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                enableAntiAliasing(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_TINY);
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        return badge;
    }

    /**
     * Aplică bara de defilare (scroll bar) modernă pe un JScrollPane.
     */
    public static void applyModernScrollBar(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_CHAT);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
    }

    /**
     * Implementare de bară de defilare minimalistă, elegantă.
     */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x4E, 0x50, 0x58);
            this.trackColor = BG_CHAT;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            enableAntiAliasing(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
}
