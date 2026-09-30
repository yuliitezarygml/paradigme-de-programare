import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

/**
 * Fereastra de dialog pentru Căutare și Înlocuire.
 *
 * Realizează cerințele din programa de laborator:
 * - Cerința b: Căutarea subsirurilor din text ținând cont de direcția de căutare (Înainte / Înapoi).
 * - Cerința c: Înlocuirea subsirurilor din text cu posibilitatea de a înlocui tot (Replace All).
 */
public class FindReplaceDialog extends JDialog {

    private final TextEditor editor;

    private final JTextField searchField = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);

    private final JCheckBox matchCaseCheck = new JCheckBox("Potrivire majuscule/minuscule (Match case)");

    // Butoanele radio pentru direcția de căutare (Cerința b)
    private final JRadioButton forwardRadio = new JRadioButton("Înainte (spre final)", true);
    private final JRadioButton backwardRadio = new JRadioButton("Înapoi (spre început)");

    private final JButton findBtn = new JButton("Găsește următorul");
    private final JButton replaceBtn = new JButton("Înlocuiește");
    private final JButton replaceAllBtn = new JButton("Înlocuiește tot");
    private final JButton closeBtn = new JButton("Închide");

    public FindReplaceDialog(TextEditor editor) {
        super(editor, "Căutare și Înlocuire", false); // false = non-modal, ca utilizatorul să poată vedea textul
        this.editor = editor;

        initComponents();
        pack();
        setLocationRelativeTo(editor);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(AppStyles.dialogBackground);

        // --- Panoul cu câmpurile de text ---
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(AppStyles.dialogBackground);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Rândul 1: Căutare
        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Ce căutăm:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(searchField, gbc);

        // Rândul 2: Înlocuire
        gbc.gridx = 0; gbc.gridy = 1;
        fieldsPanel.add(new JLabel("Cu ce înlocuim:"), gbc);
        gbc.gridx = 1;
        fieldsPanel.add(replaceField, gbc);

        // Rândul 3: Match Case
        gbc.gridx = 1; gbc.gridy = 2;
        matchCaseCheck.setBackground(AppStyles.dialogBackground);
        fieldsPanel.add(matchCaseCheck, gbc);

        // --- Panoul pentru Direcția de Căutare (Cerința b) ---
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        directionPanel.setBorder(BorderFactory.createTitledBorder("Direcția de căutare"));
        directionPanel.setBackground(AppStyles.dialogBackground);

        ButtonGroup dirGroup = new ButtonGroup();
        dirGroup.add(forwardRadio);
        dirGroup.add(backwardRadio);
        forwardRadio.setBackground(AppStyles.dialogBackground);
        backwardRadio.setBackground(AppStyles.dialogBackground);

        directionPanel.add(forwardRadio);
        directionPanel.add(backwardRadio);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        fieldsPanel.add(directionPanel, gbc);

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        // --- Panoul cu butoane de acțiune ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttonsPanel.setBackground(AppStyles.dialogBackground);

        AppStyles.applyToButton(findBtn);
        AppStyles.applyToButton(replaceBtn);
        AppStyles.applyToButton(replaceAllBtn);
        AppStyles.applyToButton(closeBtn);

        buttonsPanel.add(findBtn);
        buttonsPanel.add(replaceBtn);
        buttonsPanel.add(replaceAllBtn);
        buttonsPanel.add(closeBtn);

        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // --- Atribuire acțiuni pe butoane ---
        findBtn.addActionListener(e -> findNext(true));
        replaceBtn.addActionListener(e -> replaceCurrent());
        replaceAllBtn.addActionListener(e -> replaceAll());
        closeBtn.addActionListener(e -> setVisible(false));
    }

    /**
     * Găsește următoarea apariție a subșirului căutat în funcție de direcția selectată.
     *
     * @param notifyIfNotFound dacă se afișează mesaj când nu găsește nimic
     * @return true dacă a găsit o potrivire
     */
    public boolean findNext(boolean notifyIfNotFound) {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null) return false;

        JTextPane textPane = tab.getTextPane();
        String search = searchField.getText();

        if (search.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Introduceți textul de căutat!",
                    "Atenție", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String content;
        try {
            content = textPane.getStyledDocument().getText(0, textPane.getStyledDocument().getLength());
        } catch (Exception ex) {
            content = textPane.getText();
        }

        boolean matchCase = matchCaseCheck.isSelected();
        String sourceText = matchCase ? content : content.toLowerCase();
        String targetText = matchCase ? search : search.toLowerCase();

        boolean isForward = forwardRadio.isSelected();
        int foundPos = -1;

        if (isForward) {
            // Căutare înainte (de la sfârșitul selecției curente sau poziția cursorului)
            int startFrom = textPane.getSelectionEnd();
            foundPos = sourceText.indexOf(targetText, startFrom);

            // Dacă am ajuns la capătul documentului, reluăm căutarea de la început
            if (foundPos == -1 && startFrom > 0) {
                foundPos = sourceText.indexOf(targetText, 0);
            }
        } else {
            // Căutare înapoi (înainte de selecția curentă)
            int startFrom = textPane.getSelectionStart() - 1;
            if (startFrom >= 0) {
                foundPos = sourceText.lastIndexOf(targetText, startFrom);
            }
            // Dacă nu a găsit înainte de cursor, căutăm de la sfârșitul documentului
            if (foundPos == -1) {
                foundPos = sourceText.lastIndexOf(targetText, sourceText.length() - 1);
            }
        }

        if (foundPos != -1) {
            // Selectăm textul găsit și mutăm vizualizarea pe el
            textPane.select(foundPos, foundPos + search.length());
            textPane.requestFocusInWindow();
            return true;
        } else {
            if (notifyIfNotFound) {
                JOptionPane.showMessageDialog(this,
                        "Subșirul \"" + search + "\" nu a fost găsit în direcția selectată!",
                        "Rezultat", JOptionPane.INFORMATION_MESSAGE);
            }
            return false;
        }
    }

    /**
     * Înlocuiește subșirul curent selectat dacă acesta corespunde textului căutat,
     * apoi trece automat la următoarea apariție.
     */
    public void replaceCurrent() {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        String search = searchField.getText();
        String replace = replaceField.getText();

        if (search.isEmpty()) return;

        String selected = textPane.getSelectedText();
        boolean matchCase = matchCaseCheck.isSelected();
        boolean matches = false;

        if (selected != null) {
            matches = matchCase ? selected.equals(search) : selected.equalsIgnoreCase(search);
        }

        if (matches) {
            textPane.replaceSelection(replace);
            tab.setModified(true);
        }

        // Caută automat următorul
        findNext(true);
    }

    /**
     * Înlocuiește toate aparițiile din întregul document și afișează numărul de înlocuiri.
     * (Cerința c din laborator)
     */
    public void replaceAll() {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null) return;

        JTextPane textPane = tab.getTextPane();
        String search = searchField.getText();
        String replace = replaceField.getText();

        if (search.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduceți textul de înlocuit!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StyledDocument doc = textPane.getStyledDocument();
        boolean matchCase = matchCaseCheck.isSelected();
        String targetText = matchCase ? search : search.toLowerCase();

        int inlocuiri = 0;
        int pozitie = 0;

        try {
            while (pozitie < doc.getLength()) {
                String fullText = doc.getText(0, doc.getLength());
                String sourceText = matchCase ? fullText : fullText.toLowerCase();

                int idx = sourceText.indexOf(targetText, pozitie);
                if (idx == -1) {
                    break;
                }

                // Ștergem vechiul text și inserăm textul nou
                doc.remove(idx, search.length());
                doc.insertString(idx, replace, null);

                inlocuiri++;
                pozitie = idx + replace.length();
            }

            if (inlocuiri > 0) {
                tab.setModified(true);
                JOptionPane.showMessageDialog(this,
                        "Au fost efectuate " + inlocuiri + " înlocuiri în document!",
                        "Înlocuire completă", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Subșirul \"" + search + "\" nu a fost găsit în document!",
                        "Rezultat", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Eroare în timpul înlocuirii: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }
}
