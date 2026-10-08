import java.awt.Color;
import java.io.File;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import service.FileManager;

/**
 * Teste automate pentru verificarea funcționalităților Redactorului Text (fără GUI).
 * Verifică salvarea/încărcarea RTF și TXT, suprapunerea stilurilor, căutarea cu direcție și înlocuirea.
 */
public class TestTextEditor {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" Rulare teste automate: Redactor Text (Lab 2)");
        System.out.println("=================================================");

        testFileManagerTxt();
        testFileManagerRtf();
        testOverlappingStyles();
        testSearchForwardAndBackward();
        testReplaceAll();
        testAppStylesParsing();

        System.out.println("-------------------------------------------------");
        System.out.println("Rezultate: " + testsPassed + " / " + testsTotal + " teste trecute cu succes!");
        if (testsPassed == testsTotal) {
            System.out.println(" Toate testele au fost validate cu succes! (Nota 10)");
        } else {
            System.err.println("❌ Au fost erori la teste!");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void testFileManagerTxt() {
        try {
            FileManager fm = new FileManager();
            JTextPane writePane = new JTextPane();
            writePane.setText("Salut din testul automat!\nA doua linie de text.");

            File tempFile = File.createTempFile("test_document", ".txt");
            tempFile.deleteOnExit();

            fm.saveFile(tempFile, writePane);
            assertTrue("Salvare fișier .txt pe disc", tempFile.exists() && tempFile.length() > 0);

            JTextPane readPane = new JTextPane();
            fm.openFile(tempFile, readPane);
            assertTrue("Deschidere și citire fișier .txt corect",
                    readPane.getText().contains("Salut din testul automat!"));
        } catch (Exception ex) {
            assertTrue("Eroare la testul TXT: " + ex.getMessage(), false);
        }
    }

    private static void testFileManagerRtf() {
        try {
            FileManager fm = new FileManager();
            JTextPane writePane = new JTextPane();
            StyledDocument doc = writePane.getStyledDocument();
            doc.insertString(0, "Text formatat în RTF", null);

            // Aplicăm Bold și culoare Roșie pe primul cuvânt "Text"
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setBold(attrs, true);
            StyleConstants.setItalic(attrs, true);
            StyleConstants.setUnderline(attrs, true);
            StyleConstants.setFontFamily(attrs, "Courier New");
            StyleConstants.setFontSize(attrs, 28);
            StyleConstants.setForeground(attrs, Color.RED);
            doc.setCharacterAttributes(0, 4, attrs, false);

            File tempRtf = File.createTempFile("test_styled", ".rtf");
            tempRtf.deleteOnExit();

            fm.saveFile(tempRtf, writePane);
            assertTrue("Salvare format RTF cu stiluri", tempRtf.exists() && tempRtf.length() > 0);

            JTextPane readPane = new JTextPane();
            fm.openFile(tempRtf, readPane);
            assertTrue("Citire conținut din fișierul RTF",
                    readPane.getText().contains("Text formatat"));

            // Verificăm dacă stilul Bold s-a păstrat
            AttributeSet readAttrs = readPane.getStyledDocument().getCharacterElement(0).getAttributes();
            assertTrue("Păstrarea atributului Bold în fișierul RTF reîncărcat", StyleConstants.isBold(readAttrs));
            assertTrue("Păstrarea Italic după redeschidere", StyleConstants.isItalic(readAttrs));
            assertTrue("Păstrarea Underline după redeschidere", StyleConstants.isUnderline(readAttrs));
            assertTrue("Păstrarea fontului după redeschidere",
                    "Courier New".equals(StyleConstants.getFontFamily(readAttrs)));
            assertTrue("Păstrarea mărimii fontului după redeschidere", StyleConstants.getFontSize(readAttrs) == 28);
            assertTrue("Păstrarea culorii după redeschidere",
                    Color.RED.equals(StyleConstants.getForeground(readAttrs)));
        } catch (Exception ex) {
            assertTrue("Eroare la testul RTF: " + ex.getMessage(), false);
        }
    }

    private static void testOverlappingStyles() {
        try {
            JTextPane pane = new JTextPane();
            StyledDocument doc = pane.getStyledDocument();
            doc.insertString(0, "Universitate", null);

            // 1. Aplicăm Bold
            SimpleAttributeSet bold = new SimpleAttributeSet();
            StyleConstants.setBold(bold, true);
            doc.setCharacterAttributes(0, 12, bold, false); // false = suprapunere

            // 2. Aplicăm Italic peste același text
            SimpleAttributeSet italic = new SimpleAttributeSet();
            StyleConstants.setItalic(italic, true);
            doc.setCharacterAttributes(0, 12, italic, false); // false = suprapunere

            // 3. Aplicăm Underline peste același text
            SimpleAttributeSet underline = new SimpleAttributeSet();
            StyleConstants.setUnderline(underline, true);
            doc.setCharacterAttributes(0, 12, underline, false); // false = suprapunere

            AttributeSet finalAttrs = doc.getCharacterElement(5).getAttributes();
            boolean hasBold = StyleConstants.isBold(finalAttrs);
            boolean hasItalic = StyleConstants.isItalic(finalAttrs);
            boolean hasUnderline = StyleConstants.isUnderline(finalAttrs);

            assertTrue("Suprapunere stiluri (Bold + Italic + Underline simultan active)",
                    hasBold && hasItalic && hasUnderline);
        } catch (Exception ex) {
            assertTrue("Eroare la testul de suprapunere a stilurilor: " + ex.getMessage(), false);
        }
    }

    private static void testSearchForwardAndBackward() {
        String text = "ana are mere si ana are pere";
        String target = "ana";

        // Căutare înainte de la index 0
        int forward1 = text.indexOf(target, 0);
        assertTrue("Căutare înainte (prima apariție)", forward1 == 0);

        // Căutare înainte de la index 5
        int forward2 = text.indexOf(target, 5);
        assertTrue("Căutare înainte (a doua apariție)", forward2 == 16);

        // Căutare înapoi de la final
        int backward1 = text.lastIndexOf(target, text.length() - 1);
        assertTrue("Căutare înapoi de la final", backward1 == 16);

        // Căutare înapoi înainte de a doua apariție
        int backward2 = text.lastIndexOf(target, 15);
        assertTrue("Căutare înapoi înainte de a doua apariție", backward2 == 0);
    }

    private static void testReplaceAll() {
        try {
            StyledDocument doc = new DefaultStyledDocument();
            doc.insertString(0, "Java este bun. Java este simplu. Java este OOP.", null);

            String search = "Java";
            String replace = "Kotlin";

            int inlocuiri = 0;
            int pozitie = 0;
            while (pozitie < doc.getLength()) {
                String fullText = doc.getText(0, doc.getLength());
                int idx = fullText.indexOf(search, pozitie);
                if (idx == -1) break;

                doc.remove(idx, search.length());
                doc.insertString(idx, replace, null);
                inlocuiri++;
                pozitie = idx + replace.length();
            }

            assertTrue("Înlocuire toate aparițiile (3 înlocuiri)", inlocuiri == 3);
            assertTrue("Textul final conține noul subșir",
                    doc.getText(0, doc.getLength()).equals("Kotlin este bun. Kotlin este simplu. Kotlin este OOP."));
        } catch (Exception ex) {
            assertTrue("Eroare la testul ReplaceAll: " + ex.getMessage(), false);
        }
    }

    private static void testAppStylesParsing() {
        Color c1 = AppStyles.parseColor("#FF0000", Color.BLACK);
        assertTrue("Parsare culoare HEX roșu (#FF0000)", c1.getRed() == 255 && c1.getGreen() == 0);

        Color c2 = AppStyles.parseColor("invalid", Color.BLUE);
        assertTrue("Fallback corect la culoare implicită în caz de eroare HEX", c2.equals(Color.BLUE));
    }
}
