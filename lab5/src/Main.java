import javax.swing.*;

// Точка входа в приложение
// Лабораторная работа №5: Rețeaua Internet + Aplicație rezident (ООП на Java)
public class Main {
    public static void main(String[] args) {
        // Устанавливаем системный стиль окон
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Запуск интерфейса в потоке Swing
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
