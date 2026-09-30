import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.JToolBar;

/**
 * Clasa AppStyles gestionează încărcarea temei și a stilurilor vizuale
 * din fișierul extern "theme.properties".
 *
 * Utilizatorul poate edita fișierul theme.properties cu orice editor de text
 * pentru a schimba culorile, fonturile implicite sau dimensiunile!
 */
public class AppStyles {

    private static final Properties props = new Properties();
    private static boolean incarcat = false;

    // --- Valori implicite în caz că fișierul lipsește ---
    public static Color editorBackground = Color.WHITE;
    public static Color editorForeground = new Color(0x2C, 0x3E, 0x50);
    public static Color editorCaretColor = new Color(0x29, 0x80, 0xB9);
    public static Color editorSelectionBackground = new Color(0xB4, 0xD7, 0xFF);

    public static String defaultFontFamily = "Arial";
    public static int defaultFontSize = 14;

    public static Color toolbarBackground = new Color(0xEC, 0xEF, 0xF1);
    public static Color buttonBackground = Color.WHITE;
    public static Color buttonForeground = new Color(0x2C, 0x3E, 0x50);
    public static Color buttonBorderColor = new Color(0xCF, 0xD8, 0xDC);

    public static Color tabBackground = new Color(0xE2, 0xE8, 0xF0);
    public static Color tabSelectedBackground = Color.WHITE;
    public static Color tabForeground = new Color(0x33, 0x41, 0x55);

    public static Color statusbarBackground = new Color(0xEC, 0xEF, 0xF1);
    public static Color statusbarForeground = new Color(0x54, 0x6E, 0x7A);

    public static Color dialogBackground = new Color(0xF8, 0xFA, 0xFC);

    // Inițializare statică la prima utilizare
    static {
        loadTheme();
    }

    /**
     * Încarcă setările din theme.properties.
     * Caută fișierul în directorul curent sau în folderul proiectului.
     */
    public static void loadTheme() {
        props.clear();

        File file = new File("theme.properties");
        if (!file.exists()) {
            file = new File("lab2/theme.properties");
        }

        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
                parseProperties();
                incarcat = true;
                System.out.println("[AppStyles] Tema a fost încărcată cu succes din: " + file.getAbsolutePath());
            } catch (Exception ex) {
                System.err.println("[AppStyles] Eroare la citirea theme.properties, folosim valorile implicite: " + ex.getMessage());
            }
        } else {
            System.out.println("[AppStyles] Fișierul theme.properties nu a fost găsit, folosim valorile implicite.");
        }
    }

    /**
     * Parsează valorile din obiectul Properties și le convertește în Color și int.
     */
    private static void parseProperties() {
        editorBackground = parseColor(props.getProperty("editor.background"), editorBackground);
        editorForeground = parseColor(props.getProperty("editor.foreground"), editorForeground);
        editorCaretColor = parseColor(props.getProperty("editor.caret.color"), editorCaretColor);
        editorSelectionBackground = parseColor(props.getProperty("editor.selection.background"), editorSelectionBackground);

        defaultFontFamily = props.getProperty("editor.default.font.family", defaultFontFamily);
        try {
            String sizeStr = props.getProperty("editor.default.font.size");
            if (sizeStr != null) {
                defaultFontSize = Integer.parseInt(sizeStr.trim());
            }
        } catch (Exception ignored) {}

        toolbarBackground = parseColor(props.getProperty("toolbar.background"), toolbarBackground);
        buttonBackground = parseColor(props.getProperty("button.background"), buttonBackground);
        buttonForeground = parseColor(props.getProperty("button.foreground"), buttonForeground);
        buttonBorderColor = parseColor(props.getProperty("button.border.color"), buttonBorderColor);

        tabBackground = parseColor(props.getProperty("tab.background"), tabBackground);
        tabSelectedBackground = parseColor(props.getProperty("tab.selected.background"), tabSelectedBackground);
        tabForeground = parseColor(props.getProperty("tab.foreground"), tabForeground);

        statusbarBackground = parseColor(props.getProperty("statusbar.background"), statusbarBackground);
        statusbarForeground = parseColor(props.getProperty("statusbar.foreground"), statusbarForeground);

        dialogBackground = parseColor(props.getProperty("dialog.background"), dialogBackground);
    }

    /**
     * Transformă un cod HEX (de exemplu: "#FFFFFF" sau "#3498DB") în obiect java.awt.Color.
     */
    public static Color parseColor(String hex, Color implicit) {
        if (hex == null || hex.trim().isEmpty()) {
            return implicit;
        }
        try {
            hex = hex.trim();
            if (hex.startsWith("#")) {
                return Color.decode(hex);
            } else {
                return Color.decode("#" + hex);
            }
        } catch (Exception ex) {
            return implicit;
        }
    }

    /**
     * Aplică culorile și fontul implicit pe o componentă JTextPane.
     */
    public static void applyToEditor(JTextPane pane) {
        pane.setBackground(editorBackground);
        pane.setForeground(editorForeground);
        pane.setCaretColor(editorCaretColor);
        pane.setSelectionColor(editorSelectionBackground);
        pane.setFont(new Font(defaultFontFamily, Font.PLAIN, defaultFontSize));
    }

    /**
     * Aplică stilul vizual pe un buton din toolbar.
     */
    public static void applyToButton(AbstractButton btn) {
        btn.setBackground(buttonBackground);
        btn.setForeground(buttonForeground);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(buttonBorderColor, 1),
            BorderFactory.createEmptyBorder(4, 9, 4, 9)
        ));
    }

    /**
     * Aplică stilul pe bara de unelte (Toolbar).
     */
    public static void applyToToolbar(JToolBar toolbar) {
        toolbar.setBackground(toolbarBackground);
        toolbar.setOpaque(true);
    }
}
