import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;

/**
 * Класс, представляющий одну вкладку редактора.
 * Инкапсулирует компонент JTextPane, информацию о файле и статус изменений.
 * Принцип ООП: Инкапсуляция (Encapsulation).
 */
public class EditorTab {

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private File currentFile;
    private String title;
    private boolean isModified;
    private Runnable onModifiedChange; // Колбэк для обновления заголовка вкладки

    public EditorTab(String title, Runnable onModifiedChange) {
        this.title = title;
        this.onModifiedChange = onModifiedChange;
        this.isModified = false;
        this.currentFile = null;

        this.textPane = new JTextPane();
        this.textPane.setFont(new Font("Arial", Font.PLAIN, 15));
        this.scrollPane = new JScrollPane(textPane);

        // Слушатель изменений текста: помечает документ как измененный (точка • в заголовке)
        this.textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { markAsModified(); }
            @Override
            public void removeUpdate(DocumentEvent e) { markAsModified(); }
            @Override
            public void changedUpdate(DocumentEvent e) { markAsModified(); }
        });
    }

    private void markAsModified() {
        if (!isModified) {
            isModified = true;
            if (onModifiedChange != null) {
                onModifiedChange.run();
            }
        }
    }

    /**
     * Применение стиля к выделенному тексту (Bold, Italic, Color, Font).
     * Важно: параметр replace = false гарантирует наложение (суперпозицию) стилей!
     */
    public void applyStyleToSelection(AttributeSet attr) {
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        int length = end - start;

        if (length > 0) {
            StyledDocument doc = textPane.getStyledDocument();
            // false означает "не заменять старые стили, а объединять с ними"
            doc.setCharacterAttributes(start, length, attr, false);
            markAsModified();
        }
    }

    // Переключение жирного шрифта (Bold)
    public void toggleBold() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        boolean current = StyleConstants.isBold(getSelectedAttributes());
        StyleConstants.setBold(attr, !current);
        applyStyleToSelection(attr);
    }

    // Переключение курсива (Italic)
    public void toggleItalic() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        boolean current = StyleConstants.isItalic(getSelectedAttributes());
        StyleConstants.setItalic(attr, !current);
        applyStyleToSelection(attr);
    }

    // Переключение подчеркивания (Underline)
    public void toggleUnderline() {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        boolean current = StyleConstants.isUnderline(getSelectedAttributes());
        StyleConstants.setUnderline(attr, !current);
        applyStyleToSelection(attr);
    }

    // Смена семейства шрифта
    public void changeFontFamily(String fontFamily) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attr, fontFamily);
        applyStyleToSelection(attr);
    }

    // Смена размера шрифта
    public void changeFontSize(int size) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setFontSize(attr, size);
        applyStyleToSelection(attr);
    }

    // Смена цвета текста
    public void changeFontColor(Color color) {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, color);
        applyStyleToSelection(attr);
    }

    // Получить текущие атрибуты выделения
    private AttributeSet getSelectedAttributes() {
        int pos = textPane.getSelectionStart();
        StyledDocument doc = textPane.getStyledDocument();
        return doc.getCharacterElement(pos).getAttributes();
    }

    // Геттеры и сеттеры
    public JTextPane getTextPane() {
        return textPane;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        if (file != null) {
            this.title = file.getName();
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
        if (onModifiedChange != null) {
            onModifiedChange.run();
        }
    }

    /**
     * Отображаемый заголовок с точкой для несохраненного файла (как в Notepad на image8.png)
     */
    public String getDisplayTitle() {
        return isModified ? title + " •" : title;
    }
}

