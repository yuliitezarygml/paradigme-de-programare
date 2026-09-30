import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import service.FileManager;

/**
 * Fereastra principală a aplicației "Redactor Text" (Laboratorul 2 - Paradigma OO).
 *
 * Implementează cerințele:
 * - Cerința a: Redactarea textului, salvarea și deschiderea fișierelor
 * - Cerința b: Căutarea subșirurilor cu direcție
 * - Cerința c: Înlocuirea subșirurilor (inclusiv Înlocuiește tot)
 * - Cerința d: Modificarea fontului, mărimii, culorii, bold, italic, underline (stilurile se suprapun!)
 * - Cerința e: Lucrul cu mai multe fișiere în file noi (new tabs)
 * - Cerința f: Salvarea cu păstrarea stilurilor în format RTF
 *
 * Codul este structurat curat, orientat pe obiecte, cu comentarii explicative.
 */
public class TextEditor extends JFrame {

    // Serviciul pentru lucrul cu fișierele (.rtf și .txt)
    private final FileManager fileManager = new FileManager();

    // Dialogul pentru căutare și înlocuire (Cerințele b și c)
    private FindReplaceDialog findReplaceDialog;

    // Panoul cu file / tab-uri (Cerința e)
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final List<DocumentTab> tabs = new ArrayList<>();
    private int documentCounter = 1;

    // Componente pentru bara de unelte (Toolbar)
    private JComboBox<String> fontCombo;
    private JComboBox<Integer> sizeCombo;
    private JButton boldBtn;
    private JButton italicBtn;
    private JButton underlineBtn;
    private JButton colorBtn;

    // Bara de stare de jos (Status Bar)
    private final JLabel statusLabel = new JLabel(" Pregătit");

    public TextEditor() {
        super("Redactor Text - Paradigme de Programare (Lab 2)");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 680);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);

        // La închiderea ferestrei verificăm dacă sunt fișiere nesalvate
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        // 1. Inițializăm meniul superior
        initMenuBar();

        // 2. Inițializăm bara de instrumente (Toolbar) cu butoanele de stil
        initToolBar();

        // 3. Configurăm panoul central cu file (Tabs)
        initTabs();

        // 4. Configurăm bara de stare de jos
        initStatusBar();

        // 5. Creăm dialogul de căutare
        findReplaceDialog = new FindReplaceDialog(this);

        // 6. Deschidem primul document gol implicit
        addNewEmptyTab();
    }

    /**
     * Inițializează bara de meniu (Fișier, Editare, Format, Vizualizare, Ajutor).
     */
    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int cmdKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // === Meniul FIȘIER ===
        JMenu fileMenu = new JMenu("Fișier");
        JMenuItem newItem = new JMenuItem("Filă nouă");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, cmdKey));
        newItem.addActionListener(e -> addNewEmptyTab());

        JMenuItem openItem = new JMenuItem("Deschide...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, cmdKey));
        openItem.addActionListener(e -> openFileAction());

        JMenuItem saveItem = new JMenuItem("Salvează");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, cmdKey));
        saveItem.addActionListener(e -> saveCurrentTabAction(false));

        JMenuItem saveAsItem = new JMenuItem("Salvează ca...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, cmdKey | ActionEvent.SHIFT_MASK));
        saveAsItem.addActionListener(e -> saveCurrentTabAction(true));

        JMenuItem closeTabItem = new JMenuItem("Închide fila curentă");
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, cmdKey));
        closeTabItem.addActionListener(e -> closeActiveTab());

        JMenuItem exitItem = new JMenuItem("Ieșire");
        exitItem.addActionListener(e -> confirmExit());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeTabItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // === Meniul EDITARE ===
        JMenu editMenu = new JMenu("Editare");
        JMenuItem findItem = new JMenuItem("Căutare și Înlocuire...");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, cmdKey));
        findItem.addActionListener(e -> showFindReplaceDialog());

        JMenuItem selectAllItem = new JMenuItem("Selectează tot");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, cmdKey));
        selectAllItem.addActionListener(e -> {
            DocumentTab tab = getActiveTab();
            if (tab != null) tab.getTextPane().selectAll();
        });

        editMenu.add(findItem);
        editMenu.addSeparator();
        editMenu.add(selectAllItem);
        menuBar.add(editMenu);

        // === Meniul FORMAT (Stiluri) ===
        JMenu formatMenu = new JMenu("Format");
        JMenuItem boldMenuItem = new JMenuItem("Aldin (Bold)");
        boldMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, cmdKey));
        boldMenuItem.addActionListener(e -> toggleBold());

        JMenuItem italicMenuItem = new JMenuItem("Cursiv (Italic)");
        italicMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, cmdKey));
        italicMenuItem.addActionListener(e -> toggleItalic());

        JMenuItem underlineMenuItem = new JMenuItem("Subliniat (Underline)");
        underlineMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, cmdKey));
        underlineMenuItem.addActionListener(e -> toggleUnderline());

        JMenuItem colorMenuItem = new JMenuItem("Culoare text...");
        colorMenuItem.addActionListener(e -> chooseTextColor());

        formatMenu.add(boldMenuItem);
        formatMenu.add(italicMenuItem);
        formatMenu.add(underlineMenuItem);
        formatMenu.addSeparator();
        formatMenu.add(colorMenuItem);
        menuBar.add(formatMenu);

        // === Meniul VIZUALIZARE (Reîncărcare temă din theme.properties) ===
        JMenu viewMenu = new JMenu("Vizualizare");
        JMenuItem reloadThemeItem = new JMenuItem("Reîncarcă tema din fișier (theme.properties)");
        reloadThemeItem.addActionListener(e -> reloadThemeAction());
        viewMenu.add(reloadThemeItem);
        menuBar.add(viewMenu);

        // === Meniul AJUTOR ===
        JMenu helpMenu = new JMenu("Ajutor");
        JMenuItem aboutItem = new JMenuItem("Despre program");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Inițializează bara de instrumente (Toolbar).
     * Conține butoane pentru operații rapide și controale pentru formatarea textului.
     */
    /**
     * Inițializează bara de instrumente (Toolbar).
     * Conține butoane pentru operații rapide și controale pentru formatarea textului.
     */
    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(AppStyles.toolbarBackground);
        toolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCF, 0xD8, 0xDC)),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Grup 1: Fișiere (Nou, Deschide, Salvează)
        JButton newBtn = createToolbarButton("Nou", "📄", "Creare filă nouă (Ctrl+N)");
        newBtn.addActionListener(e -> addNewEmptyTab());

        JButton openBtn = createToolbarButton("Deschide", "📂", "Deschidere fișier RTF sau TXT (Ctrl+O)");
        openBtn.addActionListener(e -> openFileAction());

        JButton saveBtn = createToolbarButton("Salvează", "💾", "Salvare fișier (Ctrl+S)");
        saveBtn.addActionListener(e -> saveCurrentTabAction(false));

        toolBar.add(newBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(openBtn);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(saveBtn);

        // Separator vertical între grupuri
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(createToolbarSeparator());
        toolBar.add(Box.createHorizontalStrut(10));

        // Grup 2: Formatare text și fonturi
        String[] fonturi = {"Arial", "Times New Roman", "Courier New", "Verdana", "Georgia", "Comic Sans MS", "Tahoma"};
        fontCombo = new JComboBox<>(fonturi);
        fontCombo.setSelectedItem(AppStyles.defaultFontFamily);
        fontCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fontCombo.setPreferredSize(new Dimension(145, 30));
        fontCombo.setMaximumSize(new Dimension(145, 30));
        fontCombo.setToolTipText("Alege familia de font");
        fontCombo.addActionListener(e -> {
            String fontAles = (String) fontCombo.getSelectedItem();
            if (fontAles != null) {
                applyFontFamily(fontAles);
            }
        });
        toolBar.add(fontCombo);
        toolBar.add(Box.createHorizontalStrut(6));

        Integer[] marimi = {10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48};
        sizeCombo = new JComboBox<>(marimi);
        sizeCombo.setSelectedItem(AppStyles.defaultFontSize);
        sizeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sizeCombo.setPreferredSize(new Dimension(80, 30));
        sizeCombo.setMaximumSize(new Dimension(80, 30));
        sizeCombo.setToolTipText("Alege mărimea fontului");
        sizeCombo.addActionListener(e -> {
            Integer marimeAleasa = (Integer) sizeCombo.getSelectedItem();
            if (marimeAleasa != null) {
                applyFontSize(marimeAleasa);
            }
        });
        toolBar.add(sizeCombo);
        toolBar.add(Box.createHorizontalStrut(8));

        // Butoane de stil: B, I, U
        boldBtn = createStyleButton("B", new Font("Serif", Font.BOLD, 15), "Aldin / Bold (Ctrl+B)");
        boldBtn.addActionListener(e -> toggleBold());
        toolBar.add(boldBtn);
        toolBar.add(Box.createHorizontalStrut(4));

        italicBtn = createStyleButton("I", new Font("Serif", Font.ITALIC, 15), "Cursiv / Italic (Ctrl+I)");
        italicBtn.addActionListener(e -> toggleItalic());
        toolBar.add(italicBtn);
        toolBar.add(Box.createHorizontalStrut(4));

        underlineBtn = createStyleButton("U", new Font("Serif", Font.PLAIN, 15), "Subliniat / Underline (Ctrl+U)");
        underlineBtn.addActionListener(e -> toggleUnderline());
        toolBar.add(underlineBtn);
        toolBar.add(Box.createHorizontalStrut(8));

        colorBtn = createToolbarButton("Culoare", "🎨", "Selectare culoare text");
        colorBtn.addActionListener(e -> chooseTextColor());
        toolBar.add(colorBtn);

        // Separator vertical între grupuri
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(createToolbarSeparator());
        toolBar.add(Box.createHorizontalStrut(10));

        // Grup 3: Căutare și Înlocuire
        JButton findBtn = createToolbarButton("Caută / Înlocuiește", "🔍", "Deschide dialogul de căutare (Ctrl+F)");
        findBtn.addActionListener(e -> showFindReplaceDialog());
        toolBar.add(findBtn);

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolbarButton(String text, String icon, String tooltip) {
        JButton btn = new JButton(icon != null ? icon + " " + text : text);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        AppStyles.applyToButton(btn);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 10, 30));
        btn.setMaximumSize(new Dimension(btn.getPreferredSize().width + 10, 30));
        return btn;
    }

    private JButton createStyleButton(String text, Font font, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        AppStyles.applyToButton(btn);
        btn.setPreferredSize(new Dimension(32, 30));
        btn.setMaximumSize(new Dimension(32, 30));
        return btn;
    }

    private JComponent createToolbarSeparator() {
        JPanel sep = new JPanel();
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setMaximumSize(new Dimension(1, 24));
        sep.setBackground(new Color(0xD0, 0xD7, 0xDE));
        return sep;
    }

    /**
     * Inițializează panoul cu tab-uri.
     */
    private void initTabs() {
        tabbedPane.setUI(new CustomTabbedPaneUI());
        tabbedPane.setBackground(AppStyles.tabBackground);
        tabbedPane.setFocusable(false);
        tabbedPane.addChangeListener(e -> updateStatusBar());
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Inițializează bara de stare inferioară.
     */
    private void initStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        statusPanel.setBackground(AppStyles.statusbarBackground);
        statusLabel.setForeground(AppStyles.statusbarForeground);
        statusPanel.add(statusLabel, BorderLayout.WEST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    // =========================================================================
    // GESTIONAREA FILELOR / TAB-URILOR (Cerința e)
    // =========================================================================

    /**
     * Adaugă o nouă filă goală ("Document 1", "Document 2" etc.)
     */
    public DocumentTab addNewEmptyTab() {
        String titlu = "Document " + (documentCounter++);
        DocumentTab tab = new DocumentTab(titlu, null);
        addTabToPane(tab);
        return tab;
    }

    /**
     * Înregistrează o filă în JTabbedPane și creează antetul cu butonul de închidere [×].
     */
    private void addTabToPane(DocumentTab tab) {
        tabs.add(tab);
        tabbedPane.addTab("", tab.getScrollPane());

        int index = tabbedPane.indexOfComponent(tab.getScrollPane());
        tabbedPane.setTabComponentAt(index, createTabHeader(tab));
        tabbedPane.setSelectedComponent(tab.getScrollPane());

        // Actualizăm titlul cu asterisc când documentul se modifică
        tab.setOnModifiedStateChanged(() -> {
            int idx = tabs.indexOf(tab);
            if (idx != -1 && idx < tabbedPane.getTabCount()) {
                JPanel header = (JPanel) tabbedPane.getTabComponentAt(idx);
                if (header != null && header.getComponentCount() > 0) {
                    JLabel lbl = (JLabel) header.getComponent(0);
                    lbl.setText("📄 " + tab.getDisplayTitle());
                }
            }
        });

        // Actualizăm bara de stare la mișcarea cursorului
        tab.getTextPane().addCaretListener(e -> updateStatusBar());

        updateStatusBar();
    }

    /**
     * Construiește componenta personalizată de antet pentru tab (titlu + buton mic '×').
     */
    private JPanel createTabHeader(DocumentTab tab) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("📄 " + tab.getDisplayTitle());
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(AppStyles.tabForeground);

        JLabel closeBtn = new JLabel("×");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        closeBtn.setForeground(new Color(0x7F, 0x8C, 0x8D));
        closeBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        closeBtn.setToolTipText("Închide această filă (Ctrl+W)");
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                closeTab(tab);
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(new Color(0xE7, 0x4C, 0x3C)); // Roșu la hover
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(new Color(0x7F, 0x8C, 0x8D));
            }
        });

        headerPanel.add(titleLabel);
        headerPanel.add(closeBtn);
        return headerPanel;
    }

    /**
     * Închide o filă specificată, cerând confirmare dacă sunt modificări nesalvate.
     */
    public void closeTab(DocumentTab tab) {
        if (tab.isModified()) {
            int optiune = JOptionPane.showConfirmDialog(
                this,
                "Documentul \"" + tab.getTitle() + "\" conține modificări nesalvate.\nSalvați modificările înainte de închidere?",
                "Salvare document",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (optiune == JOptionPane.YES_OPTION) {
                boolean salvat = saveTab(tab, false);
                if (!salvat) return; // Dacă utilizatorul a anulat dialogul de salvare, nu închidem fila
            } else if (optiune == JOptionPane.CANCEL_OPTION || optiune == JOptionPane.CLOSED_OPTION) {
                return; // Anulare
            }
        }

        int index = tabs.indexOf(tab);
        if (index != -1) {
            tabbedPane.remove(index);
            tabs.remove(index);
        }

        // Dacă au fost închise toate filele, creăm una nouă automată
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

    // =========================================================================
    // OPERAȚII CU FIȘIERE (Cerințele a și f)
    // =========================================================================

    /**
     * Acțiune pentru deschiderea unui fișier de pe disc (RTF sau TXT).
     */
    public void openFileAction() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter rtfFilter = new FileNameExtensionFilter("Documente RTF cu stiluri (*.rtf)", "rtf");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Documente text simple (*.txt)", "txt");
        chooser.addChoosableFileFilter(rtfFilter);
        chooser.addChoosableFileFilter(txtFilter);
        chooser.setFileFilter(rtfFilter);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            try {
                // Dacă fila curentă este nouă, goală și nemodificată, o refolosim
                DocumentTab active = getActiveTab();
                DocumentTab tabToUse;
                if (active != null && active.getCurrentFile() == null && !active.isModified()
                        && active.getTextPane().getText().trim().isEmpty()) {
                    tabToUse = active;
                } else {
                    tabToUse = addNewEmptyTab();
                }

                tabToUse.setSuppressModifiedEvents(true);
                fileManager.openFile(selectedFile, tabToUse.getTextPane());
                tabToUse.setCurrentFile(selectedFile);
                tabToUse.setSuppressModifiedEvents(false);
                tabToUse.setModified(false);

                // Actualizăm antetul tab-ului
                int idx = tabs.indexOf(tabToUse);
                if (idx != -1) {
                    JPanel header = (JPanel) tabbedPane.getTabComponentAt(idx);
                    if (header != null) {
                        JLabel lbl = (JLabel) header.getComponent(0);
                        lbl.setText(tabToUse.getDisplayTitle());
                    }
                }

                updateStatusBar();
                JOptionPane.showMessageDialog(this,
                    "Fișierul a fost deschis cu succes:\n" + selectedFile.getName(),
                    "Succes", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Eroare la deschiderea fișierului:\n" + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Salvează fila activă curentă.
     */
    public boolean saveCurrentTabAction(boolean saveAs) {
        DocumentTab tab = getActiveTab();
        if (tab == null) return false;
        return saveTab(tab, saveAs);
    }

    /**
     * Salvează o filă specifică pe disc.
     */
    public boolean saveTab(DocumentTab tab, boolean forceSaveAs) {
        File targetFile = tab.getCurrentFile();

        if (targetFile == null || forceSaveAs) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter rtfFilter = new FileNameExtensionFilter("Document RTF cu stiluri (*.rtf)", "rtf");
            FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Document Text (*.txt)", "txt");
            chooser.addChoosableFileFilter(rtfFilter);
            chooser.addChoosableFileFilter(txtFilter);
            chooser.setFileFilter(rtfFilter);

            // Sugerăm un nume inițial
            chooser.setSelectedFile(new File(tab.getTitle().replace(" *", "") + ".rtf"));

            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                targetFile = chooser.getSelectedFile();

                // Adăugăm automat extensia .rtf dacă utilizatorul nu a scris-o
                String lower = targetFile.getName().toLowerCase();
                if (!lower.endsWith(".rtf") && !lower.endsWith(".txt")) {
                    if (chooser.getFileFilter() == txtFilter) {
                        targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".txt");
                    } else {
                        targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".rtf");
                    }
                }
            } else {
                return false; // Utilizatorul a anulat salvarea
            }
        }

        try {
            fileManager.saveFile(targetFile, tab.getTextPane());
            tab.setCurrentFile(targetFile);
            tab.setModified(false);

            // Actualizăm antetul tab-ului
            int idx = tabs.indexOf(tab);
            if (idx != -1) {
                JPanel header = (JPanel) tabbedPane.getTabComponentAt(idx);
                if (header != null) {
                    JLabel lbl = (JLabel) header.getComponent(0);
                    lbl.setText(tab.getDisplayTitle());
                }
            }

            updateStatusBar();
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Eroare la salvarea fișierului:\n" + ex.getMessage(),
                "Eroare", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // =========================================================================
    // MODIFICAREA STILURILOR PE SUBȘIRURI (Cerința d)
    // =========================================================================

    /**
     * Metodă principală pentru aplicarea atributelor stilistice pe subșiruri selectate.
     *
     * IMPORTANT (Cerința d):
     * Al doilea parametru `replace = false` în `setCharacterAttributes` este secretul
     * prin care stilurile se SUPRAPUN!
     * De exemplu, aplicarea culorii roșii nu șterge îngroșarea (Bold) sau cursivul (Italic).
     */
    private void applyAttributeToSelection(SimpleAttributeSet attrs) {
        DocumentTab tab = getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        int length = end - start;

        if (length > 0) {
            // Se aplică pe subșirul selectat de utilizator
            // false = atributele se suprapun (nu le șterg pe cele existente)
            textPane.getStyledDocument().setCharacterAttributes(start, length, attrs, false);
            tab.setModified(true);
        } else {
            // Dacă nu e nimic selectat, setăm stilul pentru noul text introdus de acum înainte
            textPane.setCharacterAttributes(attrs, false);
        }
        textPane.requestFocusInWindow();
    }

    /**
     * Schimbă familia de font pe subșirul selectat.
     */
    public void applyFontFamily(String fontFamily) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, fontFamily);
        applyAttributeToSelection(attrs);
    }

    /**
     * Schimbă mărimea fontului pe subșirul selectat.
     */
    public void applyFontSize(int size) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontSize(attrs, size);
        applyAttributeToSelection(attrs);
    }

    /**
     * Activează / dezactivează stilul ALDIN (Bold) pe subșirul selectat.
     */
    public void toggleBold() {
        DocumentTab tab = getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        int start = textPane.getSelectionStart();
        AttributeSet currentAttrs = textPane.getStyledDocument().getCharacterElement(start).getAttributes();
        boolean isBold = StyleConstants.isBold(currentAttrs);

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, !isBold);
        applyAttributeToSelection(attrs);
    }

    /**
     * Activează / dezactivează stilul CURSIV (Italic) pe subșirul selectat.
     */
    public void toggleItalic() {
        DocumentTab tab = getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        int start = textPane.getSelectionStart();
        AttributeSet currentAttrs = textPane.getStyledDocument().getCharacterElement(start).getAttributes();
        boolean isItalic = StyleConstants.isItalic(currentAttrs);

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, !isItalic);
        applyAttributeToSelection(attrs);
    }

    /**
     * Activează / dezactivează stilul SUBLINIAT (Underline) pe subșirul selectat.
     */
    public void toggleUnderline() {
        DocumentTab tab = getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        int start = textPane.getSelectionStart();
        AttributeSet currentAttrs = textPane.getStyledDocument().getCharacterElement(start).getAttributes();
        boolean isUnderline = StyleConstants.isUnderline(currentAttrs);

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setUnderline(attrs, !isUnderline);
        applyAttributeToSelection(attrs);
    }

    /**
     * Deschide selectorul de culori (JColorChooser) și aplică culoarea aleasă pe subșir.
     */
    public void chooseTextColor() {
        Color aleasa = JColorChooser.showDialog(this, "Alege culoarea textului", Color.RED);
        if (aleasa != null) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, aleasa);
            applyAttributeToSelection(attrs);
        }
    }

    // =========================================================================
    // CĂUTARE ȘI ÎNLOCUIRE (Cerințele b și c)
    // =========================================================================

    public void showFindReplaceDialog() {
        if (findReplaceDialog == null) {
            findReplaceDialog = new FindReplaceDialog(this);
        }
        findReplaceDialog.setVisible(true);
        findReplaceDialog.toFront();
    }

    // =========================================================================
    // ACTUALIZARE TEMĂ ȘI STARE
    // =========================================================================

    /**
     * Reîncarcă tema vizuală din fișierul extern theme.properties la cererea utilizatorului.
     */
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

    /**
     * Actualizează informațiile afișate în bara de stare (Linia, Coloana, Caractere, Format).
     */
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
        int col = caret - root.getElement(line - 1).getStartOffset() + 1;
        int totalChars = pane.getDocument().getLength();

        String format = (tab.getCurrentFile() != null && tab.getCurrentFile().getName().toLowerCase().endsWith(".rtf"))
                ? "Format: RTF (Rich Text)"
                : "Format: Text Simplu";

        statusLabel.setText(String.format(" Linia: %d  |  Coloana: %d  |  Caractere: %d  |  %s",
                line, col, totalChars, format));
    }

    /**
     * Dialogul "Despre program" cu detalierea cerințelor realizate.
     */
    private void showAboutDialog() {
        String msg = "<html><body style='width: 320px; font-family: sans-serif;'>"
                + "<h2 style='color: #2980b9;'>Redactor Text (Java Swing)</h2>"
                + "<p><b>Curs:</b> Paradigme de Programare</p>"
                + "<p><b>Laborator:</b> Nr. 2 (Paradigma Orientată pe Obiecte)</p>"
                + "<hr>"
                + "<p><b>Cerințe implementate (10 / 10 puncte):</b></p>"
                + "<ul>"
                + "<li><b>a)</b> Redactare, salvare și deschidere fișiere</li>"
                + "<li><b>b)</b> Căutare subșiruri cu direcție (Înainte / Înapoi)</li>"
                + "<li><b>c)</b> Înlocuire subșiruri și 'Înlocuiește tot'</li>"
                + "<li><b>d)</b> Modificare font, mărime, culoare, bold, italic, underline cu <i>suprapunere de stiluri</i></li>"
                + "<li><b>e)</b> Lucrul cu mai multe fișiere în file noi (new tabs)</li>"
                + "<li><b>f)</b> Salvarea și deschiderea cu păstrarea stilurilor în format RTF</li>"
                + "</ul>"
                + "<p><i>Personalizare:</i> Puteți edita oricând fișierul <b>theme.properties</b>!</p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(this, msg, "Despre Redactor Text", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Confirmă ieșirea din aplicație dacă există modificări nesalvate în vreun tab.
     */
    private void confirmExit() {
        boolean areNesalvate = false;
        for (DocumentTab tab : tabs) {
            if (tab.isModified()) {
                areNesalvate = true;
                break;
            }
        }

        if (areNesalvate) {
            int opt = JOptionPane.showConfirmDialog(
                this,
                "Există documente cu modificări nesalvate.\nDoriți să ieșiți fără a salva?",
                "Confirmare ieșire",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (opt != JOptionPane.YES_OPTION) {
                return;
            }
        }

        dispose();
        System.exit(0);
    }
}
