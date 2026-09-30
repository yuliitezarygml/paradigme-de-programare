import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Класс для работы с файлами (открытие и сохранение).
 * Поддерживает как обычный текст (.txt), так и формат со стилями (.rtf).
 * Принцип ООП: Инкапсуляция и разделение обязанностей (Single Responsibility).
 */
public class FileManager {

    private final RTFEditorKit rtfKit = new RTFEditorKit();

    /**
     * Открытие файла и загрузка содержимого в StyledDocument.
     * @param file открываемый файл
     * @param doc документ вкладки (JTextPane)
     * @throws Exception при ошибке чтения
     */
    public void openFile(File file, StyledDocument doc) throws Exception {
        // Очищаем текущий документ
        doc.remove(0, doc.getLength());

        if (file.getName().toLowerCase().endsWith(".rtf")) {
            // Читаем как RTF со всеми стилями, шрифтами и цветами
            try (InputStream in = new FileInputStream(file)) {
                rtfKit.read(in, doc, 0);
            }
        } else {
            // Читаем как обычный текстовый файл (TXT)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                doc.insertString(0, sb.toString(), null);
            }
        }
    }

    /**
     * Сохранение StyledDocument в файл.
     * @param file файл для сохранения
     * @param doc документ со стилями
     * @throws Exception при ошибке записи
     */
    public void saveFile(File file, StyledDocument doc) throws Exception {
        if (file.getName().toLowerCase().endsWith(".rtf")) {
            // Сохраняем в формате RTF с сохранением шрифтов, цветов, bold, italic
            try (OutputStream out = new FileOutputStream(file)) {
                rtfKit.write(out, doc, 0, doc.getLength());
            }
        } else {
            // Сохраняем как обычный текст
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                String text = doc.getText(0, doc.getLength());
                writer.write(text);
            }
        }
    }
}

