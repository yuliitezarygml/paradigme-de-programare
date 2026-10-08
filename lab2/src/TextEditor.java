import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import service.FileManager;

/**
 * Главное окно редактора.
 *
 * Каждое требование методички — отдельный метод:
 * a) openFileAction / saveTab          — открыть и сохранить файл
 * b, c) FindReplaceDialog              — поиск и замена
 * d) applyAttributeToSelection         — стили накладываются, а не затирают друг друга
 * e) addNewEmptyTab / closeTab         — несколько файлов во вкладках
 * f) FileManager.saveFile(".rtf")      — стили сохраняются в RTF
 */
public class TextEditor extends JFrame {

    private final FileManager fileManager = new FileManager();
    private FindReplaceDialog findReplaceDialog;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final List<DocumentTab> tabs = new ArrayList<>();
    private int documentCounter = 1;

    private JComboBox<String> fontCombo;
    private JComboBox<Integer> sizeCombo;
    /** Пока true, смена пункта в списке только показывает стиль, а не применяет его заново. */
    private boolean ignoreStyleEvents;
    private final JLabel statusLabel = new JLabel(" Pregătit");

    public TextEditor() {
        super("Redactor Text - Paradigme de Programare (Lab 2)");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(950, 680);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        initMenuBar();
        initToolBar();
        initTabs();
        initStatusBar();
        findReplaceDialog = new FindReplaceDialog(this);
        addNewEmptyTab();
    }

    // -------------------------------------------------------------------------
    // Меню и панель кнопок. Они только вызывают методы ниже.
    // -------------------------------------------------------------------------

    private void initMenuBar() {
        int cmd = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("Fișier");
        file.add(item("Filă nouă", KeyEvent.VK_N, cmd, e -> addNewEmptyTab()));
        file.add(item("Deschide...", KeyEvent.VK_O, cmd, e -> openFileAction()));
        file.addSeparator();
        file.add(item("Salvează", KeyEvent.VK_S, cmd, e -> saveCurrentTabAction(false)));
        file.add(item("Salvează ca...", KeyEvent.VK_S, cmd | ActionEvent.SHIFT_MASK, e -> saveCurrentTabAction(true)));
        file.addSeparator();
        file.add(item("Închide fila curentă", KeyEvent.VK_W, cmd, e -> closeActiveTab()));
        file.add(item("Ieșire", 0, 0, e -> confirmExit()));
        bar.add(file);

        JMenu edit = new JMenu("Editare");
        edit.add(item("Căutare și Înlocuire...", KeyEvent.VK_F, cmd, e -> showFindReplaceDialog()));
        edit.addSeparator();
        edit.add(item("Selectează tot", KeyEvent.VK_A, cmd, e -> {
            DocumentTab tab = getActiveTab();
            if (tab != null) {
                tab.getTextPane().selectAll();
            }
        }));
        bar.add(edit);

        JMenu format = new JMenu("Format");
        format.add(item("Aldin (Bold)", KeyEvent.VK_B, cmd, e -> toggleBold()));
        format.add(item("Cursiv (Italic)", KeyEvent.VK_I, cmd, e -> toggleItalic()));
        format.add(item("Subliniat (Underline)", KeyEvent.VK_U, cmd, e -> toggleUnderline()));
        format.addSeparator();
        format.add(item("Culoare text...", 0, 0, e -> chooseTextColor()));
        bar.add(format);

        JMenu view = new JMenu("Vizualizare");
        view.add(item("Reîncarcă tema din fișier (theme.properties)", 0, 0, e -> reloadThemeAction()));
        bar.add(view);

        JMenu help = new JMenu("Ajutor");
        help.add(item("Despre program", 0, 0, e -> showAboutDialog()));
        bar.add(help);

        setJMenuBar(bar);
    }

    private JMenuItem item(String title, int key, int mask, java.awt.event.ActionListener action) {
        JMenuItem menuItem = new JMenuItem(title);
        if (key != 0) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(key, mask));
        }
        menuItem.addActionListener(action);
        return menuItem;
    }

    private void initToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        AppStyles.applyToToolbar(bar);

        bar.add(toolButton("Nou", e -> addNewEmptyTab()));
        bar.add(toolButton("Deschide", e -> openFileAction()));
        bar.add(toolButton("Salvează", e -> saveCurrentTabAction(false)));
        bar.addSeparator();

        String[] systemFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontCombo = new JComboBox<>(systemFonts);
        fontCombo.setMaximumRowCount(14);
        fontCombo.setPrototypeDisplayValue("Times New Roman");
        fontCombo.setPreferredSize(new Dimension(190, 28));
        fontCombo.setMaximumSize(new Dimension(190, 28));
        fontCombo.setToolTipText("Toate fonturile instalate în sistem");
        selectFont(AppStyles.defaultFontFamily);
        fontCombo.addActionListener(e -> {
            if (ignoreStyleEvents) {
                return;
            }
            String font = (String) fontCombo.getSelectedItem();
            if (font != null) {
                applyFontFamily(font);
            }
        });
        bar.add(fontCombo);

        sizeCombo = new JComboBox<>(new Integer[]{8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72});
        sizeCombo.setPreferredSize(new Dimension(70, 28));
        sizeCombo.setMaximumSize(new Dimension(70, 28));
        selectSize(AppStyles.defaultFontSize);
        sizeCombo.addActionListener(e -> {
            if (ignoreStyleEvents) {
                return;
            }
            Integer size = (Integer) sizeCombo.getSelectedItem();
            if (size != null) {
                applyFontSize(size);
            }
        });
        bar.add(sizeCombo);

        bar.add(toolButton("B", e -> toggleBold()));
        bar.add(toolButton("I", e -> toggleItalic()));
        bar.add(toolButton("U", e -> toggleUnderline()));
        bar.add(toolButton("Culoare", e -> chooseTextColor()));
        bar.addSeparator();
        bar.add(toolButton("Caută", e -> showFindReplaceDialog()));

        add(bar, BorderLayout.NORTH);
    }

    private JButton toolButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        AppStyles.applyToButton(button);
        button.addActionListener(action);
        return button;
    }

    private void initTabs() {
        tabbedPane.setUI(new CustomTabbedPaneUI());
        tabbedPane.setBackground(AppStyles.tabBackground);
        tabbedPane.addChangeListener(e -> updateStatusBar());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppStyles.statusbarBackground);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        statusLabel.setForeground(AppStyles.statusbarForeground);
        panel.add(statusLabel, BorderLayout.WEST);
        add(panel, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // e) Вкладки: каждая вкладка — отдельный DocumentTab
    // -------------------------------------------------------------------------

    /** Новая пустая вкладка: Document 1, Document 2, ... */
    public DocumentTab addNewEmptyTab() {
        DocumentTab tab = new DocumentTab("Document " + (documentCounter++), null);
        tabs.add(tab);
        tabbedPane.addTab("", tab.getScrollPane());

        int index = tabs.size() - 1;
        tabbedPane.setTabComponentAt(index, tabHeader(tab));
        tabbedPane.setSelectedIndex(index);

        // Звёздочка в заголовке появляется, когда текст изменился.
        tab.setOnModifiedStateChanged(() -> refreshTabTitle(tab));
        tab.getTextPane().addCaretListener(e -> {
            updateStatusBar();
            syncStyleControls();
        });
        updateStatusBar();
        syncStyleControls();
        return tab;
    }

    /** Заголовок вкладки: имя файла и кнопка ×. */
    private JPanel tabHeader(DocumentTab tab) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);

        JLabel title = new JLabel(tab.getDisplayTitle());
        title.setForeground(AppStyles.tabForeground);

        JButton close = new JButton("×");
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.addActionListener(e -> closeTab(tab));

        header.add(title);
        header.add(close);
        return header;
    }

    private void refreshTabTitle(DocumentTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) {
            return;
        }
        JPanel header = (JPanel) tabbedPane.getTabComponentAt(index);
        if (header != null && header.getComponent(0) instanceof JLabel) {
            ((JLabel) header.getComponent(0)).setText(tab.getDisplayTitle());
        }
    }

    /** Закрыть вкладку. Если есть несохранённый текст — спросить. */
    public void closeTab(DocumentTab tab) {
        if (tab.isModified()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Documentul \"" + tab.getTitle() + "\" conține modificări nesalvate.\nSalvați modificările înainte de închidere?",
                "Salvare document",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                if (!saveTab(tab, false)) {
                    return;
                }
            } else if (choice != JOptionPane.NO_OPTION) {
                return;
            }
        }

        int index = tabs.indexOf(tab);
        if (index >= 0) {
            tabbedPane.remove(index);
            tabs.remove(index);
        }
        if (tabs.isEmpty()) {
            addNewEmptyTab();
        } else {
            updateStatusBar();
        }
    }

    public void closeActiveTab() {
        DocumentTab tab = getActiveTab();
        if (tab != null) {
            closeTab(tab);
        }
    }

    public DocumentTab getActiveTab() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < tabs.size()) {
            return tabs.get(index);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // a, f) Открыть и сохранить. RTF хранит стили, TXT — только текст.
    // -------------------------------------------------------------------------

    public void openFileAction() {
        JFileChooser chooser = fileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        DocumentTab tab = emptyTabOrNew();
        try {
            tab.setSuppressModifiedEvents(true);
            fileManager.openFile(file, tab.getTextPane());
            tab.attachDocumentListener();
            tab.setCurrentFile(file);
            tab.setSuppressModifiedEvents(false);
            tab.setModified(false);
            refreshTabTitle(tab);
            updateStatusBar();
            syncStyleControls();
        } catch (Exception ex) {
            tab.setSuppressModifiedEvents(false);
            JOptionPane.showMessageDialog(this,
                "Eroare la deschiderea fișierului:\n" + ex.getMessage(),
                "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Пустую несохранённую вкладку переиспользуем, иначе открываем новую. */
    private DocumentTab emptyTabOrNew() {
        DocumentTab active = getActiveTab();
        if (active != null && active.getCurrentFile() == null && !active.isModified()
                && active.getTextPane().getText().trim().isEmpty()) {
            return active;
        }
        return addNewEmptyTab();
    }

    public boolean saveCurrentTabAction(boolean saveAs) {
        DocumentTab tab = getActiveTab();
        if (tab == null) {
            return false;
        }
        return saveTab(tab, saveAs);
    }

    /**
     * Сохраняет вкладку.
     * saveAs = true или файл ещё не выбран — спрашиваем имя.
     * Расширение .rtf сохраняет стили, .txt сохраняет только символы.
     */
    public boolean saveTab(DocumentTab tab, boolean forceSaveAs) {
        File file = tab.getCurrentFile();
        if (file == null || forceSaveAs) {
            file = askSaveFile(tab);
            if (file == null) {
                return false;
            }
        }
        try {
            fileManager.saveFile(file, tab.getTextPane());
            tab.setCurrentFile(file);
            tab.setModified(false);
            refreshTabTitle(tab);
            updateStatusBar();
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Eroare la salvarea fișierului:\n" + ex.getMessage(),
                "Eroare", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private File askSaveFile(DocumentTab tab) {
        JFileChooser chooser = fileChooser();
        chooser.setSelectedFile(new File(tab.getTitle() + ".rtf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        String name = file.getName().toLowerCase();
        if (name.endsWith(".rtf") || name.endsWith(".txt")) {
            return file;
        }
        String extension = chooser.getFileFilter().getDescription().contains(".txt") ? ".txt" : ".rtf";
        return new File(file.getParentFile(), file.getName() + extension);
    }

    private JFileChooser fileChooser() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter rtf = new FileNameExtensionFilter("Documente RTF cu stiluri (*.rtf)", "rtf");
        FileNameExtensionFilter txt = new FileNameExtensionFilter("Documente text simple (*.txt)", "txt");
        chooser.addChoosableFileFilter(rtf);
        chooser.addChoosableFileFilter(txt);
        chooser.setFileFilter(rtf);
        return chooser;
    }

    // -------------------------------------------------------------------------
    // d) Стили. false = новый стиль добавляется к старым, а не заменяет их.
    // -------------------------------------------------------------------------

    /**
     * Главный метод стилей.
     * Есть выделение — стиль ставится на эти символы.
     * Нет выделения — стиль ставится на текст, который напечатают дальше.
     */
    private void applyAttributeToSelection(SimpleAttributeSet attrs) {
        DocumentTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        JTextPane pane = tab.getTextPane();
        int start = pane.getSelectionStart();
        int length = pane.getSelectionEnd() - start;
        if (length > 0) {
            pane.getStyledDocument().setCharacterAttributes(start, length, attrs, false);
            tab.setModified(true);
        } else {
            pane.setCharacterAttributes(attrs, false);
        }
        pane.requestFocusInWindow();
    }

    /**
     * Показывает в списках шрифт и размер того места, где стоит курсор.
     * После открытия RTF здесь видно то, что было сохранено.
     */
    private void syncStyleControls() {
        DocumentTab tab = getActiveTab();
        if (tab == null || fontCombo == null) {
            return;
        }
        JTextPane pane = tab.getTextPane();
        AttributeSet attrs = pane.getDocument().getLength() == 0
            ? pane.getInputAttributes()
            : styleAtCaret(pane);

        ignoreStyleEvents = true;
        selectFont(StyleConstants.getFontFamily(attrs));
        selectSize(StyleConstants.getFontSize(attrs));
        ignoreStyleEvents = false;
    }

    private AttributeSet styleAtCaret(JTextPane pane) {
        int pos = pane.getSelectionStart();
        if (pos == pane.getSelectionEnd() && pos > 0) {
            pos--;
        }
        if (pos >= pane.getDocument().getLength()) {
            pos = Math.max(0, pane.getDocument().getLength() - 1);
        }
        return pane.getStyledDocument().getCharacterElement(pos).getAttributes();
    }

    private void selectFont(String family) {
        if (family == null || family.isEmpty()) {
            return;
        }
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) fontCombo.getModel();
        if (model.getIndexOf(family) < 0) {
            model.addElement(family);
        }
        fontCombo.setSelectedItem(family);
    }

    private void selectSize(int size) {
        DefaultComboBoxModel<Integer> model = (DefaultComboBoxModel<Integer>) sizeCombo.getModel();
        if (model.getIndexOf(size) < 0) {
            model.addElement(size);
        }
        sizeCombo.setSelectedItem(size);
    }

    public void applyFontFamily(String fontFamily) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, fontFamily);
        applyAttributeToSelection(attrs);
    }

    public void applyFontSize(int size) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontSize(attrs, size);
        applyAttributeToSelection(attrs);
    }

    public void toggleBold() {
        toggleFlag("bold");
    }

    public void toggleItalic() {
        toggleFlag("italic");
    }

    public void toggleUnderline() {
        toggleFlag("underline");
    }

    /** Смотрит, включён ли стиль в позиции курсора, и ставит противоположное. */
    private void toggleFlag(String kind) {
        DocumentTab tab = getActiveTab();
        if (tab == null) {
            return;
        }
        JTextPane pane = tab.getTextPane();
        AttributeSet current = pane.getStyledDocument()
            .getCharacterElement(pane.getSelectionStart())
            .getAttributes();

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        if (kind.equals("bold")) {
            StyleConstants.setBold(attrs, !StyleConstants.isBold(current));
        } else if (kind.equals("italic")) {
            StyleConstants.setItalic(attrs, !StyleConstants.isItalic(current));
        } else {
            StyleConstants.setUnderline(attrs, !StyleConstants.isUnderline(current));
        }
        applyAttributeToSelection(attrs);
    }

    public void chooseTextColor() {
        Color color = JColorChooser.showDialog(this, "Alege culoarea textului", Color.RED);
        if (color == null) {
            return;
        }
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        applyAttributeToSelection(attrs);
    }

    // -------------------------------------------------------------------------
    // b, c) Поиск и замена живут в FindReplaceDialog
    // -------------------------------------------------------------------------

    public void showFindReplaceDialog() {
        findReplaceDialog.setVisible(true);
        findReplaceDialog.toFront();
    }

    public void reloadThemeAction() {
        AppStyles.loadTheme();
        for (DocumentTab tab : tabs) {
            AppStyles.applyToEditor(tab.getTextPane());
        }
        tabbedPane.setBackground(AppStyles.tabBackground);
        statusLabel.setForeground(AppStyles.statusbarForeground);
        repaint();
        JOptionPane.showMessageDialog(this,
            "Tema a fost reîncărcată cu succes din fișierul theme.properties!",
            "Reîncărcare stiluri", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStatusBar() {
        DocumentTab tab = getActiveTab();
        if (tab == null) {
            statusLabel.setText(" Niciun document deschis");
            return;
        }
        JTextPane pane = tab.getTextPane();
        int caret = pane.getCaretPosition();
        Element root = pane.getDocument().getDefaultRootElement();
        int line = root.getElementIndex(caret) + 1;
        int column = caret - root.getElement(line - 1).getStartOffset() + 1;
        String format = "Format: Text Simplu";
        if (tab.getCurrentFile() != null && tab.getCurrentFile().getName().toLowerCase().endsWith(".rtf")) {
            format = "Format: RTF (Rich Text)";
        }
        statusLabel.setText(" Linia: " + line + "  |  Coloana: " + column
            + "  |  Caractere: " + pane.getDocument().getLength() + "  |  " + format);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
            "Redactor Text — Laboratorul 2\n\n"
                + "a) deschidere și salvare\n"
                + "b) căutare înainte / înapoi\n"
                + "c) înlocuire și înlocuiește tot\n"
                + "d) stiluri care se suprapun\n"
                + "e) mai multe file\n"
                + "f) stiluri în format RTF",
            "Despre program",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void confirmExit() {
        for (DocumentTab tab : tabs) {
            if (!tab.isModified()) {
                continue;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                "Există documente cu modificări nesalvate.\nDoriți să ieșiți fără a salva?",
                "Confirmare ieșire",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            break;
        }
        dispose();
        System.exit(0);
    }
}
