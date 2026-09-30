import server.ServerGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punctul de intrare pentru aplicația de Server (Server Chat Local).
 * Lansează interfața grafică modernă de administrare a rețelei.
 */
public class ServerMain {
    public static void main(String[] args) {
        // Optimizări pentru randare grafică de înaltă rezoluție și fonturi netede
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "Server Chat Local");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGui = new ServerGUI();
            serverGui.setVisible(true);
        });
    }
}
