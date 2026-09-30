import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Панель навигации (кнопки навигации, адресная строка Omnibox, кнопки истории и избранного)
public class NavigationBar extends JPanel {

    public interface NavListener {
        void onBack();
        void onForward();
        void onRefresh();
        void onStop();
        void onHome();
        void onNavigate(String url);
        void onAddFavorite();
        void onShowFavorites();
        void onShowHistory();
    }

    private final NavListener listener;

    private JButton btnBack;
    private JButton btnForward;
    private JButton btnRefresh;
    private JButton btnStop;
    private JButton btnHome;
    private JTextField urlField;
    private JLabel btnStar;
    private JButton btnGo;

    public NavigationBar(NavListener listener) {
        super(new BorderLayout(8, 0));
        this.listener = listener;

        setBorder(new EmptyBorder(5, 8, 5, 8));
        setBackground(new Color(243, 243, 245));

        initUI();
    }

    private void initUI() {
        // 1. Кнопки навигации слева: ⬅ ➡ 🔄 ⏹ 🏠
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        leftPanel.setOpaque(false);

        btnBack = createBtn("⬅️", "Înapoi (Back)", e -> { if (listener != null) listener.onBack(); });
        btnForward = createBtn("➡️", "Înainte (Forward)", e -> { if (listener != null) listener.onForward(); });
        btnRefresh = createBtn("🔄", "Reîmprospătează (Refresh)", e -> { if (listener != null) listener.onRefresh(); });
        btnStop = createBtn("⏹️", "Oprește încărcarea (Stop)", e -> { if (listener != null) listener.onStop(); });
        btnHome = createBtn("🏠", "Acasă (Home)", e -> { if (listener != null) listener.onHome(); });

        leftPanel.add(btnBack);
        leftPanel.add(btnForward);
        leftPanel.add(btnRefresh);
        leftPanel.add(btnStop);
        leftPanel.add(btnHome);

        // 2. Центральная поисковая строка (Omnibox)
        JPanel omniBox = new JPanel(new BorderLayout(6, 0));
        omniBox.setBackground(Color.WHITE);
        omniBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 205), 1, true),
            new EmptyBorder(3, 8, 3, 6)
        ));

        JLabel lockIcon = new JLabel("🔒");
        lockIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lockIcon.setForeground(new Color(110, 110, 110));

        urlField = new JTextField();
        urlField.setBorder(null);
        urlField.setFont(new Font("Arial", Font.PLAIN, 13));
        urlField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && listener != null) {
                    listener.onNavigate(urlField.getText().trim());
                }
            }
        });

        btnStar = new JLabel(" ⭐ ");
        btnStar.setToolTipText("Adaugă la Favorite");
        btnStar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (listener != null) listener.onAddFavorite();
            }
        });

        omniBox.add(lockIcon, BorderLayout.WEST);
        omniBox.add(urlField, BorderLayout.CENTER);
        omniBox.add(btnStar, BorderLayout.EAST);

        // 3. Кнопки справа: Go 🔍, ⭐ Favorite, 📜 Istoric
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        rightPanel.setOpaque(false);

        btnGo = new JButton("Go 🔍");
        btnGo.setFont(new Font("Arial", Font.BOLD, 12));
        btnGo.setMargin(new Insets(2, 6, 2, 6));
        btnGo.addActionListener(e -> {
            if (listener != null) listener.onNavigate(urlField.getText().trim());
        });

        JButton btnFav = createBtn("⭐ Favorite", "Colecție favorite", e -> { if (listener != null) listener.onShowFavorites(); });
        JButton btnHist = createBtn("📜 Istoric", "Istoric navigare", e -> { if (listener != null) listener.onShowHistory(); });

        rightPanel.add(btnGo);
        rightPanel.add(btnFav);
        rightPanel.add(btnHist);

        add(leftPanel, BorderLayout.WEST);
        add(omniBox, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    private JButton createBtn(String text, String tooltip, java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.PLAIN, 12));
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.addActionListener(action);
        return b;
    }

    public void setUrl(String url) {
        urlField.setText(url != null ? url : "");
        urlField.setCaretPosition(0);
    }

    public void setBackEnabled(boolean enabled) { btnBack.setEnabled(enabled); }
    public void setForwardEnabled(boolean enabled) { btnForward.setEnabled(enabled); }

    public void setLoading(boolean loading) {
        btnStop.setEnabled(loading);
        btnRefresh.setEnabled(!loading);
    }
}
