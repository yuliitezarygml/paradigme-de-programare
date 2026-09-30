import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

// Верхняя панель вкладок (в стиле Google Chrome / Safari)
// Располагается в самом верху окна, вкладки выровнены по левому краю
public class TabBar extends JPanel {

    public interface TabBarListener {
        void onSelectTab(int index);
        void onCloseTab(int index);
        void onNewTab();
    }

    private final TabBarListener listener;
    private final JPanel tabsContainer;

    public TabBar(TabBarListener listener) {
        super(new BorderLayout());
        this.listener = listener;

        // Фон верхней строки вкладок (как в Chrome)
        setBackground(new Color(222, 225, 230));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 203, 208)));

        tabsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        tabsContainer.setOpaque(false);

        add(tabsContainer, BorderLayout.WEST);
    }

    // Перерисовать список вкладок
    public void updateTabs(List<BrowserTab> tabs, int selectedIndex) {
        tabsContainer.removeAll();

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            BrowserTab tab = tabs.get(i);
            boolean isSelected = (i == selectedIndex);

            // Панель вкладки
            JPanel tabChip = new JPanel(new BorderLayout(6, 0));
            Color bgNormal = isSelected ? Color.WHITE : new Color(230, 233, 237);
            Color bgHover = isSelected ? Color.WHITE : new Color(238, 240, 243);
            tabChip.setBackground(bgNormal);

            tabChip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, isSelected ? 0 : 1, 1, new Color(195, 198, 202)),
                new EmptyBorder(3, 8, 3, 6)
            ));
            tabChip.setPreferredSize(new Dimension(210, 32));
            tabChip.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Название страницы
            String title = tab.getTitle();
            if (title == null || title.trim().isEmpty()) title = "Pagină nouă";
            String shortTitle = title.length() > 22 ? title.substring(0, 20) + "..." : title;

            JLabel titleLbl = new JLabel("🌐 " + shortTitle);
            titleLbl.setFont(new Font("Arial", isSelected ? Font.BOLD : Font.PLAIN, 12));
            titleLbl.setForeground(isSelected ? new Color(30, 30, 30) : new Color(80, 80, 80));
            titleLbl.setToolTipText(title);

            // Кнопка закрытия вкладки "×"
            JLabel closeBtn = new JLabel(" × ");
            closeBtn.setFont(new Font("Arial", Font.BOLD, 14));
            closeBtn.setForeground(new Color(120, 120, 120));
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setToolTipText("Închide fila");
            closeBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    closeBtn.setForeground(new Color(220, 50, 50));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    closeBtn.setForeground(new Color(120, 120, 120));
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (listener != null) listener.onCloseTab(index);
                }
            });

            // Клик по вкладке переключает на нее
            MouseAdapter selectAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (listener != null) listener.onSelectTab(index);
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelected) tabChip.setBackground(bgHover);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isSelected) tabChip.setBackground(bgNormal);
                }
            };
            tabChip.addMouseListener(selectAdapter);
            titleLbl.addMouseListener(selectAdapter);

            tabChip.add(titleLbl, BorderLayout.CENTER);
            tabChip.add(closeBtn, BorderLayout.EAST);

            tabsContainer.add(tabChip);
        }

        // Кнопка "+" для открытия новой вкладки
        JButton newTabBtn = new JButton("➕");
        newTabBtn.setFont(new Font("Arial", Font.BOLD, 12));
        newTabBtn.setPreferredSize(new Dimension(30, 30));
        newTabBtn.setMargin(new Insets(0, 0, 0, 0));
        newTabBtn.setFocusable(false);
        newTabBtn.setToolTipText("Deschide o filă nouă (+)");
        newTabBtn.addActionListener(e -> {
            if (listener != null) listener.onNewTab();
        });

        tabsContainer.add(newTabBtn);

        tabsContainer.revalidate();
        tabsContainer.repaint();
    }
}
