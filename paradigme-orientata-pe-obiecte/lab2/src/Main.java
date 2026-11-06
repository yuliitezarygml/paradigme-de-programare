import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punctul de intrare în aplicație (metoda main).
 *
 * Setează Look & Feel-ul nativ al sistemului de operare (pentru un aspect modern pe macOS/Windows/Linux)
 * și lansează fereastra principală a redactorului text pe firul de execuție grafic (Event Dispatch Thread).
 */
public class Main {

    public static void main(String[] args) {
        // Rulăm pe firul de execuție al interfeței grafice Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Setăm aspectul nativ al sistemului de operare
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Dacă nu reușește, continuă cu tema implicită Java Metal
                System.out.println("Nu s-a putut aplica SystemLookAndFeel, folosim cel implicit: " + ex.getMessage());
            }

            // Creăm și afișăm fereastra redactorului de text
            TextEditor editor = new TextEditor();
            editor.setVisible(true);
        });
    }
}
