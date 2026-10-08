import java.io.File;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Одна вкладка.
 * Хранит текст (JTextPane), файл на диске и флаг «есть несохранённые правки».
 */
public class DocumentTab {

    private final JTextPane textPane = new JTextPane();
    private final JScrollPane scrollPane;
    private File currentFile;
    private String title;
    private boolean isModified;
    private boolean suppressModifiedEvents;
    private Runnable onModifiedStateChanged;

    public DocumentTab(String initialTitle, File file) {
        this.title = initialTitle;
        this.currentFile = file;
        AppStyles.applyToEditor(textPane);
        // Стиль нового текста. Он записывается в символы и потом сохраняется в RTF.
        MutableAttributeSet typing = textPane.getInputAttributes();
        StyleConstants.setFontFamily(typing, AppStyles.defaultFontFamily);
        StyleConstants.setFontSize(typing, AppStyles.defaultFontSize);
        StyleConstants.setForeground(typing, AppStyles.editorForeground);
        scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        attachDocumentListener();
    }

    /**
     * Следит за правками текущего документа.
     * После открытия файла документ заменяется, поэтому слушатель вешаем заново.
     */
    public void attachDocumentListener() {
        textPane.getStyledDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                markModified();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                markModified();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                markModified();
            }
        });
    }

    /** Первая правка ставит звёздочку. При загрузке файла события глушим. */
    private void markModified() {
        if (!suppressModifiedEvents && !isModified) {
            setModified(true);
        }
    }

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

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
        if (onModifiedStateChanged != null) {
            onModifiedStateChanged.run();
        }
    }

    public void setSuppressModifiedEvents(boolean suppress) {
        this.suppressModifiedEvents = suppress;
    }

    public void setOnModifiedStateChanged(Runnable callback) {
        this.onModifiedStateChanged = callback;
    }

    /** Имя на вкладке. Звёздочка значит «не сохранено». */
    public String getDisplayTitle() {
        return isModified ? title + " *" : title;
    }
}
