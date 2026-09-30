import javax.swing.*;

/**
 * Точка входа для запуска сетевого чата.
 * Лабораторная работа №3: Rețeaua locală (ООП на Java).
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ChatWindow window = new ChatWindow();
            window.setVisible(true);
        });
    }
}
