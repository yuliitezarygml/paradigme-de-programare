import client.ClientGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа для запуска Клиентского приложения локального чата (Chat Client Local).
 * Инициализирует и отображает современный графический интерфейс пользователя.
 */
public class ClientMain {
    public static void main(String[] args) {
        // Настройки для четкого рендеринга шрифтов и аппаратного сглаживания графики
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "Клиент локального чата");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Запуск интерфейса в потоке диспетчеризации событий Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGui = new ClientGUI();
            clientGui.setVisible(true);
        });
    }
}
