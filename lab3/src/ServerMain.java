import server.ServerGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа для запуска Сервера локального чата (Server Chat Local).
 * Инициализирует и отображает современную графическую панель администратора.
 */
public class ServerMain {
    public static void main(String[] args) {
        // Настройки для четкого рендеринга шрифтов и сглаживания текста на всех дисплеях (включая Retina)
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "Сервер локального чата");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Запуск графического интерфейса в потоке диспетчеризации событий Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGui = new ServerGUI();
            serverGui.setVisible(true);
        });
    }
}
