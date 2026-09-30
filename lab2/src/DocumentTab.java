import java.io.File;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Clasa DocumentTab reprezintă o singură filă (tab) deschisă în redactor.
 * Conține componenta de editare JTextPane, referința la fișierul de pe disc
 * și starea curentă a modificărilor (isModified).
 *
 * Realizează Cerința e din programa de laborator:
 * "Lucrul cu mai multe fișiere în file noi aparte (new tabs)"
 */
public class DocumentTab {

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private File currentFile;
    private String title;
    private boolean isModified = false;
    private boolean suppressModifiedEvents = false;

    // Listener pentru a notifica fereastra principală la orice modificare
    private Runnable onModifiedStateChanged;

    public DocumentTab(String initialTitle, File file) {
        this.title = initialTitle;
        this.currentFile = file;

        this.textPane = new JTextPane();
        AppStyles.applyToEditor(this.textPane);

        this.scrollPane = new JScrollPane(this.textPane);
        this.scrollPane.setBorder(null);

        // Atașăm ascultător de evenimente pe conținutul documentului
        attachDocumentListener();
    }

    /**
     * Ascultă modificările aduse textului și stilurilor pentru a marca
     * documentul ca "modificat" (asterisc în titlu).
     */
    public void attachDocumentListener() {
        textPane.getStyledDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Modificare de atribute/stiluri
                notifyChange();
            }
        });
    }

    private void notifyChange() {
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

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        if (currentFile != null) {
            this.title = currentFile.getName();
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

    /**
     * Titlul afișat pe fila din tab, cu asterisc dacă este modificat.
     */
    public String getDisplayTitle() {
        if (isModified) {
            return title + " *";
        }
        return title;
    }
}
