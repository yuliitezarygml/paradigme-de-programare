import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JScrollPane;
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
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicButtonUI;
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
    private final JPanel tabStrip = new JPanel();
    private final JScrollPane tabScroll = new JScrollPane(tabStrip);
    private final List<DocumentTab> tabs = new ArrayList<>();
    private int documentCounter = 1;

    private JComboBox<String> fontCombo;
    private JComboBox<Integer> sizeCombo;
    private JToggleButton boldBtn;
    private JToggleButton italicBtn;
    private JToggleButton underlineBtn;
    private JButton colorBtn;
    private Color currentTextColor = AppStyles.editorForeground;
    /** Пока true, смена пункта в списке только показывает стиль, а не применяет его заново. */
    private boolean ignoreStyleEvents;
    private int lastCaretDot = -1;
    private int lastCaretMark = -1;
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

        boldBtn = styleToggle("B", Font.BOLD);
        boldBtn.addActionListener(e -> toggleBold());
        bar.add(boldBtn);

        italicBtn = styleToggle("I", Font.ITALIC);
        italicBtn.addActionListener(e -> toggleItalic());
        bar.add(italicBtn);

        underlineBtn = styleToggle("U", Font.PLAIN);
        underlineBtn.setFont(underlinedFont());
        underlineBtn.addActionListener(e -> toggleUnderline());
        bar.add(underlineBtn);

        colorBtn = toolButton("Culoare", e -> chooseTextColor());
        paintColorButton();
        bar.add(colorBtn);
        bar.addSeparator();
        bar.add(toolButton("Caută", e -> showFindReplaceDialog()));

        add(bar, BorderLayout.NORTH);
    }

    private JButton toolButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        AppStyles.applyToButton(button);
        // BasicButtonUI рисует фон. На macOS стандартная кнопка его прячет.
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setFocusable(false);
        button.addChangeListener(e -> {
            if (button.getModel().isPressed()) {
                button.setBackground(new Color(0x29, 0x80, 0xB9));
                button.setForeground(Color.WHITE);
            } else if (button == colorBtn) {
                paintColorButton();
            } else {
                button.setBackground(AppStyles.buttonBackground);
                button.setForeground(AppStyles.buttonForeground);
            }
        });
        button.addActionListener(action);
        return button;
    }

    /** Кнопка стиля остаётся синей, пока этот стиль включён у курсора. */
    private JToggleButton styleToggle(String text, int fontStyle) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(new Font("Serif", fontStyle, 15));
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(36, 28));
        button.setMaximumSize(new Dimension(36, 28));
        markStyleButton(button, false);
        return button;
    }

    private Font underlinedFont() {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FAMILY, "Serif");
        attrs.put(TextAttribute.SIZE, 15);
        attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        return new Font(attrs);
    }

    private void markStyleButton(JToggleButton button, boolean on) {
        button.setSelected(on);
        button.setBackground(on ? new Color(0x29, 0x80, 0xB9) : Color.WHITE);
        button.setForeground(on ? Color.WHITE : AppStyles.buttonForeground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(on ? new Color(0x1A, 0x52, 0x7A) : AppStyles.buttonBorderColor, on ? 2 : 1),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
    }

    private void paintColorButton() {
        if (colorBtn == null) {
            return;
        }
        colorBtn.setOpaque(true);
        colorBtn.setBackground(currentTextColor);
        int brightness = (currentTextColor.getRed() + currentTextColor.getGreen() + currentTextColor.getBlue()) / 3;
        colorBtn.setForeground(brightness > 160 ? AppStyles.buttonForeground : Color.WHITE);
    }

    private void initTabs() {
        tabbedPane.setUI(new CustomTabbedPaneUI());
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.addChangeListener(e -> {
            lastCaretDot = -1;
            lastCaretMark = -1;
            highlightTabs();
            updateStatusBar();
            DocumentTab tab = getActiveTab();
            if (tab != null) {
                copyCaretStyleToInput(tab.getTextPane());
                syncStyleControls();
            }
        });

        tabStrip.setLayout(new javax.swing.BoxLayout(tabStrip, javax.swing.BoxLayout.X_AXIS));
        tabStrip.setBackground(AppStyles.tabBackground);
        tabStrip.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        tabScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCF, 0xD8, 0xDC)));
        tabScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        tabScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tabScroll.getViewport().setBackground(AppStyles.tabBackground);
        tabScroll.getHorizontalScrollBar().setUnitIncrement(80);
        int barHeight = tabScroll.getHorizontalScrollBar().getPreferredSize().height;
        tabScroll.setPreferredSize(new Dimension(100, 40 + barHeight));

        JPanel center = new JPanel(new BorderLayout());
        center.add(tabScroll, BorderLayout.NORTH);
        center.add(tabbedPane, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
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
        tabbedPane.addTab(tab.getTitle(), tab.getScrollPane());
        tabStrip.add(tabChip(tab));
        tabStrip.add(Box.createHorizontalStrut(6));

        int index = tabs.size() - 1;
        tabbedPane.setSelectedIndex(index);
        highlightTabs();
        tabScroll.revalidate();
        final int opened = index;
        javax.swing.SwingUtilities.invokeLater(() -> scrollTabIntoView(opened));

        // Звёздочка в заголовке появляется, когда текст изменился.
        tab.setOnModifiedStateChanged(() -> refreshTabTitle(tab));
        tab.getTextPane().addCaretListener(e -> updateStatusBar());
        // Отдельный слушатель: кнопки обновляются только когда курсор реально сдвинулся.
        tab.getTextPane().addCaretListener(e -> {
            if (e.getDot() == lastCaretDot && e.getMark() == lastCaretMark) {
                return;
            }
            lastCaretDot = e.getDot();
            lastCaretMark = e.getMark();
            copyCaretStyleToInput(tab.getTextPane());
            syncStyleControls();
        });
        updateStatusBar();
        syncStyleControls();
        return tab;
    }

    /** Одна плашка в полосе: имя файла слева, крестик справа. */
    private JPanel tabChip(DocumentTab tab) {
        JPanel chip = new JPanel(new BorderLayout(8, 0));
        chip.setOpaque(true);
        chip.setBackground(AppStyles.tabBackground);
        chip.setBorder(chipBorder(false));

        JLabel title = new JLabel(tab.getDisplayTitle());
        title.setFont(new Font("SansSerif", Font.PLAIN, 13));
        title.setForeground(AppStyles.tabForeground);

        JButton close = new JButton("×");
        close.setFont(new Font("SansSerif", Font.BOLD, 16));
        close.setUI(new BasicButtonUI());
        close.setMargin(new java.awt.Insets(0, 0, 0, 0));
        close.setPreferredSize(new Dimension(22, 22));
        close.setMinimumSize(new Dimension(22, 22));
        close.setMaximumSize(new Dimension(22, 22));
        close.setFocusable(false);
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setContentAreaFilled(false);
        close.setOpaque(false);
        close.setForeground(new Color(0x33, 0x41, 0x55));
        close.setToolTipText("Închide");
        close.addActionListener(e -> closeTab(tab));

        java.awt.event.MouseAdapter open = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int index = tabs.indexOf(tab);
                if (index >= 0) {
                    tabbedPane.setSelectedIndex(index);
                    scrollTabIntoView(index);
                }
            }
        };
        chip.addMouseListener(open);
        title.addMouseListener(open);

        chip.add(title, BorderLayout.CENTER);
        chip.add(close, BorderLayout.EAST);
        fitChip(chip);
        return chip;
    }

    private javax.swing.border.Border chipBorder(boolean selected) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(selected ? new Color(0x29, 0x80, 0xB9) : new Color(0xCF, 0xD8, 0xDC)),
            BorderFactory.createEmptyBorder(4, 10, 4, 6)
        );
    }

    /** Ширина плашки считается уже вместе с рамкой, чтобы крестик не обрезался. */
    private void fitChip(JPanel chip) {
        chip.setPreferredSize(null);
        chip.setMaximumSize(null);
        Dimension size = chip.getPreferredSize();
        int width = size.width + 4;
        int height = 34;
        chip.setPreferredSize(new Dimension(width, height));
        chip.setMinimumSize(new Dimension(width, height));
        chip.setMaximumSize(new Dimension(width, height));
    }

    private void highlightTabs() {
        int selected = tabbedPane.getSelectedIndex();
        int chipIndex = 0;
        for (java.awt.Component component : tabStrip.getComponents()) {
            if (!(component instanceof JPanel)) {
                continue;
            }
            JPanel chip = (JPanel) component;
            boolean on = chipIndex == selected;
            chip.setBackground(on ? Color.WHITE : new Color(0xE2, 0xE8, 0xF0));
            chip.setBorder(chipBorder(on));
            chipIndex++;
        }
    }

    private void scrollTabIntoView(int index) {
        int chipIndex = 0;
        for (java.awt.Component component : tabStrip.getComponents()) {
            if (!(component instanceof JPanel)) {
                continue;
            }
            if (chipIndex == index) {
                tabStrip.scrollRectToVisible(component.getBounds());
                return;
            }
            chipIndex++;
        }
    }

    private void refreshTabTitle(DocumentTab tab) {
        int index = tabs.indexOf(tab);
        if (index < 0) {
            return;
        }
        int chipIndex = 0;
        for (java.awt.Component component : tabStrip.getComponents()) {
            if (!(component instanceof JPanel)) {
                continue;
            }
            if (chipIndex == index) {
                JPanel chip = (JPanel) component;
                if (chip.getComponent(0) instanceof JLabel) {
                    ((JLabel) chip.getComponent(0)).setText(tab.getDisplayTitle());
                }
                fitChip(chip);
                tabStrip.revalidate();
                return;
            }
            chipIndex++;
        }
    }

    private void removeChip(int index) {
        int componentIndex = index * 2;
        if (componentIndex < tabStrip.getComponentCount()) {
            tabStrip.remove(componentIndex);
        }
        if (componentIndex < tabStrip.getComponentCount()) {
            tabStrip.remove(componentIndex);
        }
        tabStrip.revalidate();
        tabStrip.repaint();
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
            removeChip(index);
            highlightTabs();
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
            lastCaretDot = -1;
            lastCaretMark = -1;
            copyCaretStyleToInput(tab.getTextPane());
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
        syncStyleControls();
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
        AttributeSet attrs;
        if (pane.getDocument().getLength() == 0 || pane.getSelectionStart() == pane.getSelectionEnd()) {
            attrs = pane.getInputAttributes();
        } else {
            attrs = pane.getStyledDocument().getCharacterElement(pane.getSelectionStart()).getAttributes();
        }

        ignoreStyleEvents = true;
        selectFont(StyleConstants.getFontFamily(attrs));
        selectSize(StyleConstants.getFontSize(attrs));
        if (boldBtn != null) {
            markStyleButton(boldBtn, StyleConstants.isBold(attrs));
            markStyleButton(italicBtn, StyleConstants.isItalic(attrs));
            markStyleButton(underlineBtn, StyleConstants.isUnderline(attrs));
            currentTextColor = StyleConstants.getForeground(attrs);
            paintColorButton();
        }
        ignoreStyleEvents = false;
    }

    /** Берёт стиль символа у курсора и делает его стилем следующего ввода. */
    private void copyCaretStyleToInput(JTextPane pane) {
        if (pane.getDocument().getLength() == 0 || pane.getSelectionStart() != pane.getSelectionEnd()) {
            return;
        }
        MutableAttributeSet input = pane.getInputAttributes();
        AttributeSet saved = copyConcreteStyle(styleAtCaret(pane));
        input.removeAttributes(input);
        input.addAttributes(saved);
    }

    /** Копия значений стиля. Нельзя класть в поле ввода сам элемент документа: очистка сотрёт текст. */
    private AttributeSet copyConcreteStyle(AttributeSet from) {
        SimpleAttributeSet copy = new SimpleAttributeSet();
        StyleConstants.setBold(copy, StyleConstants.isBold(from));
        StyleConstants.setItalic(copy, StyleConstants.isItalic(from));
        StyleConstants.setUnderline(copy, StyleConstants.isUnderline(from));
        StyleConstants.setFontFamily(copy, StyleConstants.getFontFamily(from));
        StyleConstants.setFontSize(copy, StyleConstants.getFontSize(from));
        StyleConstants.setForeground(copy, StyleConstants.getForeground(from));
        return copy;
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
        AttributeSet current = pane.getSelectionStart() == pane.getSelectionEnd()
            ? pane.getInputAttributes()
            : pane.getStyledDocument().getCharacterElement(pane.getSelectionStart()).getAttributes();

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        if (kind.equals("bold")) {
            StyleConstants.setBold(attrs, !StyleConstants.isBold(current));
            StyleConstants.setForeground(attrs, Color.RED);
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
