import javafx.application.Platform;
import javax.swing.*;

// Точка входа в программу
// Лабораторная работа 4: Интернет-Браузер с поддержкой современных веб-сайтов (JavaFX WebView)
public class Main {
    public static void main(String[] args) {
        // Устанавливаем системный стиль окон (macOS / Windows / Linux)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Системный стиль не применился: " + e.getMessage());
        }

        // Чтобы JavaFX среда не закрывалась при закрытии отдельных вкладок
        Platform.setImplicitExit(false);

        // Запуск интерфейса в потоке Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            Browser browser = new Browser();
            browser.setVisible(true);
        });
    }
}
