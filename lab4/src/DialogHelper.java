import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

// Вспомогательный класс для отображения диалоговых окон (Избранное и История)
public class DialogHelper {

    // Диалог списка закладок
    public static void showFavoritesDialog(JFrame parent, FavoritesManager manager, Consumer<String> onOpen, Runnable onUpdate) {
        List<String> favs = manager.getFavorites();
        if (favs.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Lista de favorite este goală!", "Favorite", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parent, "Colecție Favorite (salvate în favorites.txt)", true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(parent);

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String f : favs) model.addElement(f);

        JList<String> list = new JList<>(model);
        list.setFont(new Font("Arial", Font.PLAIN, 13));

        JButton btnOpen = new JButton("Deschide în filă");
        btnOpen.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                if (onOpen != null) onOpen.accept(selected);
            }
        });

        JButton btnDelete = new JButton("Șterge");
        btnDelete.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                manager.removeFavorite(selected);
                model.removeElement(selected);
                if (onUpdate != null) onUpdate.run();
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnOpen);
        btns.add(btnDelete);

        dialog.add(new JScrollPane(list), BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Диалог истории посещений
    public static void showHistoryDialog(JFrame parent, HistoryManager manager, Consumer<String> onOpen) {
        List<HistoryManager.Entry> hist = manager.getHistory();
        if (hist.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Istoricul este gol!", "Istoric", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parent, "Istoricul paginilor vizitate (History)", true);
        dialog.setSize(520, 350);
        dialog.setLocationRelativeTo(parent);

        DefaultListModel<HistoryManager.Entry> model = new DefaultListModel<>();
        for (HistoryManager.Entry h : hist) model.addElement(h);

        JList<HistoryManager.Entry> list = new JList<>(model);
        list.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton btnOpen = new JButton("Deschide pagina");
        btnOpen.addActionListener(e -> {
            HistoryManager.Entry selected = list.getSelectedValue();
            if (selected != null) {
                dialog.dispose();
                if (onOpen != null) onOpen.accept(selected.getUrl());
            }
        });

        JButton btnClear = new JButton("Curăță tot");
        btnClear.addActionListener(e -> {
            manager.clear();
            model.clear();
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnOpen);
        btns.add(btnClear);

        dialog.add(new JScrollPane(list), BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
