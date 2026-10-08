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
 * Открытие и сохранение файлов.
 * .rtf хранит текст и всё, что поставлено на символы: шрифт, размер, цвет, жирный, курсив, подчёркивание.
 * .txt хранит только символы, без стилей.
 */
public class FileManager {

    private final RTFEditorKit rtfKit = new RTFEditorKit();

    public void openFile(File file, JTextPane textPane) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Fișierul specificat nu există!");
        }
        if (file.getName().toLowerCase().endsWith(".rtf")) {
            StyledDocument doc = new DefaultStyledDocument();
            try (FileInputStream in = new FileInputStream(file)) {
                rtfKit.read(in, doc, 0);
            }
            textPane.setStyledDocument(doc);
            return;
        }

        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(line);
            }
        }
        StyledDocument doc = new DefaultStyledDocument();
        doc.insertString(0, text.toString(), null);
        textPane.setStyledDocument(doc);
    }

    public void saveFile(File file, JTextPane textPane) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("Nu a fost specificat fișierul pentru salvare!");
        }
        StyledDocument doc = textPane.getStyledDocument();
        if (file.getName().toLowerCase().endsWith(".rtf")) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                rtfKit.write(out, doc, 0, doc.getLength());
            }
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(textPane.getText());
        }
    }
}
