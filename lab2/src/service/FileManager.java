package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * Clasa FileManager este responsabilă de deschiderea și salvarea fișierelor.
 *
 * Realizează cerințele din programa de laborator:
 * - Cerința a: Salvarea și deschiderea fișierelor (TXT și RTF).
 * - Cerința f: Salvarea fișierelor cu păstrarea stilurilor (format RTF).
 *
 * Scrisă simplu și clar, într-un stil accesibil pentru studenți.
 */
public class FileManager {

    // Kit-ul oficial din biblioteca standard Java pentru lucrul cu fișiere RTF
    private final RTFEditorKit rtfKit = new RTFEditorKit();

    /**
     * Deschide un fișier de pe disc și îl încarcă în JTextPane.
     * Dacă fișierul este de tip .rtf, folosește RTFEditorKit pentru a păstra
     * toate stilurile de formatare (culori, fonturi, bold, italic etc.).
     *
     * @param file fișierul selectat de utilizator
     * @param textPane componenta vizuală unde se afișează textul
     * @throws Exception dacă apare vreo eroare la citire
     */
    public void openFile(File file, JTextPane textPane) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Fișierul specificat nu există!");
        }

        String nume = file.getName().toLowerCase();

        if (nume.endsWith(".rtf")) {
            // --- 1. Citire fișier RTF (cu stiluri) ---
            StyledDocument doc = new DefaultStyledDocument();
            try (FileInputStream fis = new FileInputStream(file)) {
                // RTFEditorKit citește octeții și reconstruiește atributele stilistice
                rtfKit.read(fis, doc, 0);
            }
            textPane.setStyledDocument(doc);
        } else {
            // --- 2. Citire fișier text simplu (.txt sau altă extensie) ---
            StringBuilder continut = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String linie;
                boolean primaLinie = true;
                while ((linie = reader.readLine()) != null) {
                    if (!primaLinie) {
                        continut.append("\n");
                    }
                    continut.append(linie);
                    primaLinie = false;
                }
            }

            // Creăm un document nou și inserăm textul simplu
            StyledDocument doc = new DefaultStyledDocument();
            doc.insertString(0, continut.toString(), null);
            textPane.setStyledDocument(doc);
        }
    }

    /**
     * Salvează conținutul din JTextPane într-un fișier pe disc.
     * Dacă fișierul are extensia .rtf, toate stilurile de text se salvează în format RTF.
     *
     * @param file fișierul destinație
     * @param textPane componenta vizuală cu textul
     * @throws Exception dacă apare vreo eroare la scriere
     */
    public void saveFile(File file, JTextPane textPane) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("Nu a fost specificat fișierul pentru salvare!");
        }

        String nume = file.getName().toLowerCase();

        if (nume.endsWith(".rtf")) {
            // --- 1. Salvare în format RTF (Cerința f din laborator) ---
            StyledDocument doc = textPane.getStyledDocument();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                // RTFEditorKit exportă arborele de atribute în format standard RTF
                rtfKit.write(fos, doc, 0, doc.getLength());
            }
        } else {
            // --- 2. Salvare în format text simplu (.txt) ---
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(textPane.getText());
            }
        }
    }
}
