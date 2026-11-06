import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Окно поиска и замены.
 *
 * b) findNext     — ищет вперёд (indexOf) или назад (lastIndexOf)
 * c) replaceCurrent / replaceAll — заменяет одно совпадение или все
 */
public class FindReplaceDialog extends JDialog {

    private final TextEditor editor;
    private final JTextField searchField = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);
    private final JCheckBox matchCaseCheck = new JCheckBox("Potrivire majuscule/minuscule");
    private final JRadioButton forwardRadio = new JRadioButton("Înainte (spre final)", true);
    private final JRadioButton backwardRadio = new JRadioButton("Înapoi (spre început)");

    public FindReplaceDialog(TextEditor editor) {
        super(editor, "Căutare și Înlocuire", false);
        this.editor = editor;

        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.setBackground(AppStyles.dialogBackground);
        form.add(new JLabel("Ce căutăm:"));
        form.add(searchField);
        form.add(new JLabel("Cu ce înlocuim:"));
        form.add(replaceField);
        form.add(new JLabel(""));
        matchCaseCheck.setBackground(AppStyles.dialogBackground);
        form.add(matchCaseCheck);

        ButtonGroup direction = new ButtonGroup();
        direction.add(forwardRadio);
        direction.add(backwardRadio);
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        directionPanel.setBorder(BorderFactory.createTitledBorder("Direcția de căutare"));
        directionPanel.setBackground(AppStyles.dialogBackground);
        forwardRadio.setBackground(AppStyles.dialogBackground);
        backwardRadio.setBackground(AppStyles.dialogBackground);
        directionPanel.add(forwardRadio);
        directionPanel.add(backwardRadio);
        form.add(new JLabel(""));
        form.add(directionPanel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(AppStyles.dialogBackground);
        buttons.add(button("Găsește următorul", e -> findNext(true)));
        buttons.add(button("Înlocuiește", e -> replaceCurrent()));
        buttons.add(button("Înlocuiește tot", e -> replaceAll()));
        buttons.add(button("Închide", e -> setVisible(false)));

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(AppStyles.dialogBackground);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(editor);
    }

    private JButton button(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        AppStyles.applyToButton(button);
        button.addActionListener(action);
        return button;
    }

    /**
     * b) Поиск с направлением.
     * Вперёд: indexOf от конца текущего выделения, если не нашли — с начала текста.
     * Назад: lastIndexOf до курсора, если не нашли — с конца текста.
     */
    public boolean findNext(boolean notifyIfNotFound) {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null) {
            return false;
        }
        String search = searchField.getText();
        if (search.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduceți textul de căutat!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        JTextPane pane = tab.getTextPane();
        String content;
        try {
            content = pane.getStyledDocument().getText(0, pane.getStyledDocument().getLength());
        } catch (Exception ex) {
            content = pane.getText();
        }
        boolean matchCase = matchCaseCheck.isSelected();
        String source = matchCase ? content : content.toLowerCase();
        String target = matchCase ? search : search.toLowerCase();

        int found;
        if (forwardRadio.isSelected()) {
            found = source.indexOf(target, pane.getSelectionEnd());
            if (found < 0) {
                found = source.indexOf(target, 0);
            }
        } else {
            int before = pane.getSelectionStart() - 1;
            found = before >= 0 ? source.lastIndexOf(target, before) : -1;
            if (found < 0) {
                found = source.lastIndexOf(target);
            }
        }

        if (found >= 0) {
            pane.select(found, found + search.length());
            pane.requestFocusInWindow();
            return true;
        }
        if (notifyIfNotFound) {
            JOptionPane.showMessageDialog(this,
                "Subșirul \"" + search + "\" nu a fost găsit în direcția selectată!",
                "Rezultat", JOptionPane.INFORMATION_MESSAGE);
        }
        return false;
    }

    /**
     * Удаляет найденный кусок и вставляет новый текст с тем же оформлением.
     * Стиль берётся до удаления и сразу записывается на каждый новый символ.
     */
    private void replaceKeepingStyle(StyledDocument doc, int index, int oldLength, String replacement) throws Exception {
        AttributeSet style = strongestStyle(doc, index, oldLength);
        doc.remove(index, oldLength);
        if (replacement.isEmpty()) {
            return;
        }
        doc.insertString(index, replacement, style);
        doc.setCharacterAttributes(index, replacement.length(), style, true);
    }

    /** Если в слове стили стоят не на первой букве, берём самый «сильный» символ. */
    private AttributeSet strongestStyle(StyledDocument doc, int index, int length) {
        AttributeSet best = copyStyle(doc, index);
        int bestScore = styleScore(best);
        for (int i = 1; i < length; i++) {
            AttributeSet next = copyStyle(doc, index + i);
            int score = styleScore(next);
            if (score > bestScore) {
                best = next;
                bestScore = score;
            }
        }
        return best;
    }

    private int styleScore(AttributeSet attrs) {
        int score = 0;
        if (StyleConstants.isBold(attrs)) score += 4;
        if (StyleConstants.isItalic(attrs)) score += 2;
        if (StyleConstants.isUnderline(attrs)) score += 2;
        if (StyleConstants.getFontSize(attrs) != AppStyles.defaultFontSize) score += 1;
        java.awt.Color color = StyleConstants.getForeground(attrs);
        if (color != null && !color.equals(java.awt.Color.BLACK) && !color.equals(AppStyles.editorForeground)) {
            score += 8;
        }
        return score;
    }

    /**
     * Жирный, курсив, подчёркивание, шрифт, размер и цвет.
     * Копируем сами значения: после удаления фрагмента старая ссылка на стиль уже пустая.
     */
    private AttributeSet copyStyle(StyledDocument doc, int index) {
        AttributeSet from = doc.getCharacterElement(index).getAttributes();
        SimpleAttributeSet copy = new SimpleAttributeSet();
        StyleConstants.setBold(copy, StyleConstants.isBold(from));
        StyleConstants.setItalic(copy, StyleConstants.isItalic(from));
        StyleConstants.setUnderline(copy, StyleConstants.isUnderline(from));
        StyleConstants.setFontFamily(copy, StyleConstants.getFontFamily(from));
        StyleConstants.setFontSize(copy, StyleConstants.getFontSize(from));
        StyleConstants.setForeground(copy, StyleConstants.getForeground(from));
        return copy;
    }

    /** c) Если выделение совпадает с поиском — заменяем и ищем следующее. */
    public void replaceCurrent() {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null || searchField.getText().isEmpty()) {
            return;
        }
        JTextPane pane = tab.getTextPane();
        String selected = pane.getSelectedText();
        boolean same = selected != null && (matchCaseCheck.isSelected()
            ? selected.equals(searchField.getText())
            : selected.equalsIgnoreCase(searchField.getText()));
        if (same) {
            try {
                StyledDocument doc = pane.getStyledDocument();
                int start = pane.getSelectionStart();
                int length = pane.getSelectionEnd() - start;
                replaceKeepingStyle(doc, start, length, replaceField.getText());
                pane.setCaretPosition(start + replaceField.getText().length());
                tab.setModified(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare în timpul înlocuirii: " + ex.getMessage(),
                    "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
       
        findNext(true);
    }

    /** c) Проходит по всему документу и заменяет каждое вхождение. */
    public void replaceAll() {
        DocumentTab tab = editor.getActiveTab();
        if (tab == null) {
            return;
        }
        String search = searchField.getText();
        if (search.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduceți textul de înlocuit!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StyledDocument doc = tab.getTextPane().getStyledDocument();
        String replace = replaceField.getText();
        boolean matchCase = matchCaseCheck.isSelected();
        String needle = matchCase ? search : search.toLowerCase();
        int count = 0;
        int from = 0;

        try {
            while (from < doc.getLength()) {
                String text = doc.getText(0, doc.getLength());
                String haystack = matchCase ? text : text.toLowerCase();
                int index = haystack.indexOf(needle, from);
                if (index < 0) {
                    break;
                }
                replaceKeepingStyle(doc, index, search.length(), replace);
                count++;
                from = index + replace.length();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Eroare în timpul înlocuirii: " + ex.getMessage(),
                "Eroare", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (count > 0) {
            tab.setModified(true);
            JOptionPane.showMessageDialog(this,
                "Au fost efectuate " + count + " înlocuiri în document!",
                "Înlocuire completă", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Subșirul \"" + search + "\" nu a fost găsit în document!",
                "Rezultat", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
