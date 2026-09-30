import client.ClientGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punctul de intrare pentru aplicația de Client (Chat Rețea Locală).
 * Lansează interfața grafică modernă a utilizatorului.
 */
public class ClientMain {
    public static void main(String[] args) {
        // Optimizări pentru randare grafică de înaltă calitate
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "Chat Client Local");

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGui = new ClientGUI();
            clientGui.setVisible(true);
        });
    }
}
