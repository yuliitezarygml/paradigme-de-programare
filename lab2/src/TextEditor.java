import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный класс приложения «Текстовый редактор» (Лабораторная работа 2).
 * Наследует JFrame и объединяет все компоненты: меню, тулбар форматирования,
 * вкладки (JTabbedPane), диалог поиска и менеджер файлов.
 */
public class TextEditor extends JFrame implements ActionListener {

    private final JTabbedPane tabbedPane;
    private final List<EditorTab> tabs;
    private final FileManager fileManager;
    private final FindReplaceDialog findReplaceDialog;

    // Элементы тулбара форматирования
    private JComboBox<String> fontBox;
    private JComboBox<Integer> sizeBox;
    private JButton btnBold, btnItalic, btnUnderline, btnColor;
    private JLabel statusLabel;

    private int newDocCounter = 1;

    public TextEditor() {
        setTitle("Redactor de text - Laborator 2 (OOP)");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        this.tabs = new ArrayList<>();
        this.fileManager = new FileManager();
        this.tabbedPane = new JTabbedPane();
        this.findReplaceDialog = new FindReplaceDialog(this);

        initMenuBar();
        initToolBar();

        add(tabbedPane, BorderLayout.CENTER);

        initStatusBar();

        // Создаем первую вкладку по умолчанию
        addNewTab("Document 1.txt");

        // Обновляем статусную строку при смене активной вкладки
        tabbedPane.addChangeListener(e -> updateStatus());
    }

    // ==========================================
    // Меню программы
    // ==========================================
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Меню "Fișier" (Файл)
        JMenu fileMenu = new JMenu("Fișier");
        JMenuItem itemNew = createMenuItem("Filă nouă (New Tab)", KeyEvent.VK_N, e -> addNewTab("Document " + (++newDocCounter) + ".txt"));
        JMenuItem itemOpen = createMenuItem("Deschide... (Open)", KeyEvent.VK_O, e -> openFile());
        JMenuItem itemSave = createMenuItem("Salvează (Save)", KeyEvent.VK_S, e -> saveCurrentFile(false));
        JMenuItem itemSaveAs = createMenuItem("Salvează ca... (Save As)", 0, e -> saveCurrentFile(true));
        JMenuItem itemClose = createMenuItem("Închide fila (Close Tab)", KeyEvent.VK_W, e -> closeActiveTab());
        JMenuItem itemExit = createMenuItem("Ieșire (Exit)", KeyEvent.VK_Q, e -> System.exit(0));

        fileMenu.add(itemNew);
        fileMenu.add(itemOpen);
        fileMenu.addSeparator();
        fileMenu.add(itemSave);
        fileMenu.add(itemSaveAs);
        fileMenu.addSeparator();
        fileMenu.add(itemClose);
        fileMenu.add(itemExit);

        // Меню "Editare" (Правка)
        JMenu editMenu = new JMenu("Editare");
        JMenuItem itemFind = createMenuItem("Găsire și înlocuire... (Find/Replace)", KeyEvent.VK_F, e -> showFindDialog());
        JMenuItem itemSelectAll = createMenuItem("Selectează tot (Select All)", KeyEvent.VK_A, e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) tab.getTextPane().selectAll();
        });

        editMenu.add(itemFind);
        editMenu.addSeparator();
        editMenu.add(itemSelectAll);

        // Меню "Formatare" (Формат)
        JMenu formatMenu = new JMenu("Formatare");
        JMenuItem itemBold = createMenuItem("Îngroșat (Bold)", KeyEvent.VK_B, e -> toggleBold());
        JMenuItem itemItalic = createMenuItem("Cursiv (Italic)", KeyEvent.VK_I, e -> toggleItalic());
        JMenuItem itemUnderline = createMenuItem("Subliniat (Underline)", KeyEvent.VK_U, e -> toggleUnderline());
        JMenuItem itemColor = createMenuItem("Culoare text... (Color)", 0, e -> chooseColor());

        formatMenu.add(itemBold);
        formatMenu.add(itemItalic);
        formatMenu.add(itemUnderline);
        formatMenu.addSeparator();
        formatMenu.add(itemColor);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);

        setJMenuBar(menuBar);
    }

    private JMenuItem createMenuItem(String text, int keyEvent, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (keyEvent != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        }
        item.addActionListener(listener);
        return item;
    }

    // ==========================================
    // Панель инструментов (ToolBar) для стилей
    // ==========================================
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Кнопки быстрых действий
        JButton btnNew = new JButton("+ Filă");
        btnNew.setToolTipText("Creează filă nouă");
        btnNew.addActionListener(e -> addNewTab("Document " + (++newDocCounter) + ".txt"));

        JButton btnOpen = new JButton("Deschide");
        btnOpen.addActionListener(e -> openFile());

        JButton btnSave = new JButton("Salvează");
        btnSave.addActionListener(e -> saveCurrentFile(false));

        JButton btnFind = new JButton("Caută 🔍");
        btnFind.addActionListener(e -> showFindDialog());

        toolBar.add(btnNew);
        toolBar.add(btnOpen);
        toolBar.add(btnSave);
        toolBar.add(btnFind);
        toolBar.addSeparator(new Dimension(15, 0));

        // Выбор семейства шрифта
        String[] fonts = {"Arial", "Times New Roman", "Courier New", "Verdana", "Tahoma", "Georgia"};
        fontBox = new JComboBox<>(fonts);
        fontBox.setMaximumSize(new Dimension(140, 28));
        fontBox.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) {
                tab.changeFontFamily((String) fontBox.getSelectedItem());
            }
        });
        toolBar.add(fontBox);
        toolBar.addSeparator(new Dimension(8, 0));

        // Выбор размера шрифта
        Integer[] sizes = {10, 12, 14, 16, 18, 20, 24, 28, 36, 48};
        sizeBox = new JComboBox<>(sizes);
        sizeBox.setSelectedItem(16);
        sizeBox.setMaximumSize(new Dimension(70, 28));
        sizeBox.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) {
                tab.changeFontSize((Integer) sizeBox.getSelectedItem());
            }
        });
        toolBar.add(sizeBox);
        toolBar.addSeparator(new Dimension(8, 0));

        // Кнопки Bold, Italic, Underline
        btnBold = new JButton("B");
        btnBold.setFont(new Font("Arial", Font.BOLD, 14));
        btnBold.setToolTipText("Îngroșat (Bold)");
        btnBold.addActionListener(e -> toggleBold());

        btnItalic = new JButton("I");
        btnItalic.setFont(new Font("Arial", Font.ITALIC, 14));
        btnItalic.setToolTipText("Cursiv (Italic)");
        btnItalic.addActionListener(e -> toggleItalic());

        btnUnderline = new JButton("U");
        btnUnderline.setFont(new Font("Arial", Font.PLAIN, 14));
        btnUnderline.setToolTipText("Subliniat (Underline)");
        btnUnderline.addActionListener(e -> toggleUnderline());

        btnColor = new JButton("🎨 Culoare");
        btnColor.setToolTipText("Selectează culoarea textului");
        btnColor.addActionListener(e -> chooseColor());

        toolBar.add(btnBold);
        toolBar.add(btnItalic);
        toolBar.add(btnUnderline);
        toolBar.addSeparator(new Dimension(8, 0));
        toolBar.add(btnColor);

        add(toolBar, BorderLayout.NORTH);
    }

    // Статусная строка снизу
    private void initStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        statusPanel.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("Pregătit");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // Управление вкладками (Tabs)
    // ==========================================

    public void addNewTab(String title) {
        EditorTab tab = new EditorTab(title, this::updateTabTitles);
        tabs.add(tab);

        tabbedPane.addTab(tab.getDisplayTitle(), tab.getScrollPane());
        int index = tabbedPane.getTabCount() - 1;

        // Создаем заголовок вкладки с кнопкой закрытия [x]
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabHeader.setOpaque(false);
        JLabel titleLabel = new JLabel(tab.getDisplayTitle());
        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 4, 0, 4));
        closeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusable(false);
        closeBtn.addActionListener(e -> closeTab(tab));

        tabHeader.add(titleLabel);
        tabHeader.add(closeBtn);
        tabbedPane.setTabComponentAt(index, tabHeader);

        tabbedPane.setSelectedIndex(index);

        // Слушатель каретки для обновления строки состояния
        tab.getTextPane().addCaretListener(e -> updateStatus());
    }

    private void updateTabTitles() {
        for (int i = 0; i < tabs.size(); i++) {
            EditorTab tab = tabs.get(i);
            Component header = tabbedPane.getTabComponentAt(i);
            if (header instanceof JPanel) {
                JLabel lbl = (JLabel) ((JPanel) header).getComponent(0);
                lbl.setText(tab.getDisplayTitle());
            }
        }
        updateStatus();
    }

    public void closeActiveTab() {
        EditorTab active = getActiveTab();
        if (active != null) {
            closeTab(active);
        }
    }

    private void closeTab(EditorTab tab) {
        if (tab.isModified()) {
            int res = JOptionPane.showConfirmDialog(this,
                    "Fișierul '" + tab.getTitle() + "' conține modificări nesalvate. Salvați?",
                    "Salvare fișier", JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                saveTab(tab, false);
            } else if (res == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        int index = tabs.indexOf(tab);
        if (index != -1) {
            tabs.remove(index);
            tabbedPane.remove(index);
        }

        // Если все закрыли, создаем одну пустую
        if (tabs.isEmpty()) {
            addNewTab("Document " + (++newDocCounter) + ".txt");
        }
    }

    public EditorTab getActiveTab() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < tabs.size()) {
            return tabs.get(index);
        }
        return null;
    }

    // ==========================================
    // Операции с файлами (Open, Save, Save As)
    // ==========================================

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Documente RTF și TXT (*.rtf, *.txt)", "rtf", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                // Если текущая вкладка пустая и без файла, открываем в ней
                EditorTab active = getActiveTab();
                if (active != null && active.getCurrentFile() == null && !active.isModified() && active.getTextPane().getText().isEmpty()) {
                    fileManager.openFile(file, active.getTextPane().getStyledDocument());
                    active.setCurrentFile(file);
                    active.setModified(false);
                    updateTabTitles();
                } else {
                    // Иначе создаем новую вкладку
                    EditorTab newTab = new EditorTab(file.getName(), this::updateTabTitles);
                    fileManager.openFile(file, newTab.getTextPane().getStyledDocument());
                    newTab.setCurrentFile(file);
                    newTab.setModified(false);

                    tabs.add(newTab);
                    tabbedPane.addTab(newTab.getDisplayTitle(), newTab.getScrollPane());

                    int idx = tabbedPane.getTabCount() - 1;
                    setupTabHeader(idx, newTab);
                    tabbedPane.setSelectedIndex(idx);
                }
                JOptionPane.showMessageDialog(this, "Fișierul '" + file.getName() + "' a fost deschis cu succes!", "Succes", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la deschiderea fișierului: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupTabHeader(int index, EditorTab tab) {
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabHeader.setOpaque(false);
        JLabel titleLabel = new JLabel(tab.getDisplayTitle());
        JButton closeBtn = new JButton("×");
        closeBtn.setMargin(new Insets(0, 4, 0, 4));
        closeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusable(false);
        closeBtn.addActionListener(e -> closeTab(tab));

        tabHeader.add(titleLabel);
        tabHeader.add(closeBtn);
        tabbedPane.setTabComponentAt(index, tabHeader);
    }

    private void saveCurrentFile(boolean saveAs) {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            saveTab(tab, saveAs);
        }
    }

    private void saveTab(EditorTab tab, boolean saveAs) {
        File file = tab.getCurrentFile();

        if (file == null || saveAs) {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File(tab.getTitle().replace("•", "").trim()));
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Document Rich Text (*.rtf)", "rtf"));
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("Fișier text simplu (*.txt)", "txt"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                // Автоматически добавляем расширение .rtf, если не указано
                if (!file.getName().contains(".")) {
                    file = new File(file.getAbsolutePath() + ".rtf");
                }
                tab.setCurrentFile(file);
            } else {
                return;
            }
        }

        try {
            fileManager.saveFile(file, tab.getTextPane().getStyledDocument());
            tab.setModified(false);
            updateTabTitles();
            JOptionPane.showMessageDialog(this, "Fișier salvat cu succes: " + file.getName(), "Salvare", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Eroare la salvare: " + ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // Форматирование текста (Стилизация)
    // ==========================================

    private void toggleBold() {
        EditorTab tab = getActiveTab();
        if (tab != null) tab.toggleBold();
    }

    private void toggleItalic() {
        EditorTab tab = getActiveTab();
        if (tab != null) tab.toggleItalic();
    }

    private void toggleUnderline() {
        EditorTab tab = getActiveTab();
        if (tab != null) tab.toggleUnderline();
    }

    private void chooseColor() {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            Color c = JColorChooser.showDialog(this, "Alege culoarea textului", Color.BLACK);
            if (c != null) {
                tab.changeFontColor(c);
            }
        }
    }

    // ==========================================
    // Поиск и замена
    // ==========================================

    private void showFindDialog() {
        findReplaceDialog.setVisible(true);
    }

    private void updateStatus() {
        EditorTab tab = getActiveTab();
        if (tab != null) {
            String text = tab.getTextPane().getText();
            int chars = text.length();
            int lines = text.isEmpty() ? 1 : text.split("\n", -1).length;
            String fileType = (tab.getCurrentFile() != null && tab.getCurrentFile().getName().endsWith(".rtf")) ? "RTF" : "TXT";
            statusLabel.setText(String.format("Caractere: %d | Linii: %d | Format: %s | Fila: %s", chars, lines, fileType, tab.getTitle()));
        } else {
            statusLabel.setText("Niciun document deschis");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Дополнительные обработчики при необходимости
    }
}

