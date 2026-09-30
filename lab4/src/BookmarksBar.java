import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

// Панель закладок (Bookmarks bar), располагается сразу под строкой адреса
public class BookmarksBar extends JPanel {

    private final Consumer<String> onBookmarkClick;

    public BookmarksBar(Consumer<String> onBookmarkClick) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 3));
        this.onBookmarkClick = onBookmarkClick;
        setBackground(new Color(248, 248, 250));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 225, 230)));
    }

    // Обновляет кнопки на панели закладок
    public void setBookmarks(List<String> bookmarks) {
        removeAll();

        JLabel title = new JLabel("Bookmarks: ");
        title.setFont(new Font("Arial", Font.BOLD, 11));
        title.setForeground(new Color(110, 110, 110));
        add(title);

        if (bookmarks == null || bookmarks.isEmpty()) {
            JLabel empty = new JLabel("(apăsați ⭐ pentru a salva un site)");
            empty.setFont(new Font("Arial", Font.ITALIC, 11));
            empty.setForeground(Color.GRAY);
            add(empty);
        } else {
            int count = 0;
            for (String url : bookmarks) {
                if (count++ >= 8) break; // Показываем первые 8 закладок
                String label = url.replace("https://", "").replace("http://", "").replace("www.", "");
                if (label.length() > 22) label = label.substring(0, 20) + "..";

                JButton chip = new JButton("🌐 " + label);
                chip.setFont(new Font("Arial", Font.PLAIN, 11));
                chip.setMargin(new Insets(1, 5, 1, 5));
                chip.setFocusable(false);
                chip.setToolTipText(url);
                chip.addActionListener(e -> {
                    if (onBookmarkClick != null) onBookmarkClick.accept(url);
                });
                add(chip);
            }
        }

        revalidate();
        repaint();
    }
}
