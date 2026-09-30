import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;

/**
 * Диалоговое окно для поиска и замены подстрок.
 * Соответствует требованиям:
 * - b. Căutarea cu direcție (Sus/Jos, Match case, Wrap around) [image9.png]
 * - c. Înlocuirea și selectarea tuturor aparițiilor [image1.png]
 */
public class FindReplaceDialog extends JDialog {

    private final TextEditor parentEditor;

    private JTextField findField;
    private JTextField replaceField;
    private JCheckBox matchCaseCheck;
    private JCheckBox wrapAroundCheck;
    private JRadioButton downRadio;
    private JRadioButton upRadio;

    public FindReplaceDialog(TextEditor parent) {
        super(parent, "Găsire și Înlocuire (Find & Replace)", false);
        this.parentEditor = parent;

        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Поля ввода: Найти и Заменить
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        fieldsPanel.add(new JLabel("Caută (Find):"));
        findField = new JTextField(20);
        fieldsPanel.add(findField);

        fieldsPanel.add(new JLabel("Înlocuiește cu (Replace):"));
        replaceField = new JTextField(20);
        fieldsPanel.add(replaceField);
        contentPanel.add(fieldsPanel);

        contentPanel.add(Box.createVerticalStrut(10));

        // Опции: Регистр, Зацикливание и Направление поиска
        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        matchCaseCheck = new JCheckBox("Sensibil la majuscule (Match case)");
        wrapAroundCheck = new JCheckBox("Căutare circulară (Wrap around)", true);

        downRadio = new JRadioButton("Înainte (Down)", true);
        upRadio = new JRadioButton("Înapoi (Up)");
        ButtonGroup dirGroup = new ButtonGroup();
        dirGroup.add(downRadio);
        dirGroup.add(upRadio);

        optionsPanel.add(matchCaseCheck);
        optionsPanel.add(downRadio);
        optionsPanel.add(wrapAroundCheck);
        optionsPanel.add(upRadio);
        contentPanel.add(optionsPanel);

        contentPanel.add(Box.createVerticalStrut(12));

        // Кнопки действий
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton btnFindNext = new JButton("Găsește (Find Next)");
        JButton btnReplace = new JButton("Înlocuiește (Replace)");
        JButton btnReplaceAll = new JButton("Înlocuiește tot (Replace All)");
        JButton btnSelectAll = new JButton("Evidențiază tot (Select All)");
        JButton btnClose = new JButton("Închide");

        btnFindNext.addActionListener(e -> findNext());
        btnReplace.addActionListener(e -> replaceCurrent());
        btnReplaceAll.addActionListener(e -> replaceAll());
        btnSelectAll.addActionListener(e -> highlightAll());
        btnClose.addActionListener(e -> setVisible(false));

        buttonsPanel.add(btnFindNext);
        buttonsPanel.add(btnReplace);
        buttonsPanel.add(btnReplaceAll);
        buttonsPanel.add(btnSelectAll);
        buttonsPanel.add(btnClose);

        contentPanel.add(buttonsPanel);
        setContentPane(contentPanel);
    }

    private JTextPane getActiveTextPane() {
        EditorTab tab = parentEditor.getActiveTab();
        return tab != null ? tab.getTextPane() : null;
    }

    /**
     * Поиск следующего вхождения подстроки с учетом направления (вверх/вниз).
     */
    public boolean findNext() {
        JTextPane pane = getActiveTextPane();
        if (pane == null) return false;

        String target = findField.getText();
        if (target.isEmpty()) return false;

        String content = pane.getText();
        boolean matchCase = matchCaseCheck.isSelected();
        boolean wrap = wrapAroundCheck.isSelected();
        boolean forward = downRadio.isSelected();

        String searchContent = matchCase ? content : content.toLowerCase();
        String searchTarget = matchCase ? target : target.toLowerCase();

        int caret = pane.getCaretPosition();
        int foundIndex = -1;

        if (forward) {
            // Поиск вперед (вниз)
            int startPos = pane.getSelectionEnd();
            foundIndex = searchContent.indexOf(searchTarget, startPos);
            if (foundIndex == -1 && wrap) {
                // Если не нашли и включен wrap, ищем с самого начала
                foundIndex = searchContent.indexOf(searchTarget, 0);
            }
        } else {
            // Поиск назад (вверх)
            int startPos = pane.getSelectionStart() - 1;
            if (startPos >= 0) {
                foundIndex = searchContent.lastIndexOf(searchTarget, startPos);
            }
            if (foundIndex == -1 && wrap) {
                // Ищем с самого конца
                foundIndex = searchContent.lastIndexOf(searchTarget);
            }
        }

        if (foundIndex != -1) {
            pane.select(foundIndex, foundIndex + target.length());
            pane.requestFocusInWindow();
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Subșirul '" + target + "' nu a fost găsit!", "Rezultat căutare", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    /**
     * Замена текущего выделенного вхождения и поиск следующего.
     */
    private void replaceCurrent() {
        JTextPane pane = getActiveTextPane();
        if (pane == null) return;

        String target = findField.getText();
        String replacement = replaceField.getText();
        String selected = pane.getSelectedText();

        boolean match = false;
        if (selected != null) {
            if (matchCaseCheck.isSelected()) {
                match = selected.equals(target);
            } else {
                match = selected.equalsIgnoreCase(target);
            }
        }

        if (match) {
            pane.replaceSelection(replacement);
        }

        findNext();
    }

    /**
     * Замена всех вхождений подстроки в тексте.
     */
    private void replaceAll() {
        JTextPane pane = getActiveTextPane();
        if (pane == null) return;

        String target = findField.getText();
        String replacement = replaceField.getText();
        if (target.isEmpty()) return;

        String text = pane.getText();
        int count = 0;
        int index = 0;

        boolean matchCase = matchCaseCheck.isSelected();
        String searchText = matchCase ? text : text.toLowerCase();
        String searchTarget = matchCase ? target : target.toLowerCase();

        // Считаем количество и заменяем
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while ((index = searchText.indexOf(searchTarget, lastIndex)) != -1) {
            sb.append(text, lastIndex, index);
            sb.append(replacement);
            lastIndex = index + target.length();
            count++;
        }
        sb.append(text.substring(lastIndex));

        if (count > 0) {
            pane.setText(sb.toString());
            JOptionPane.showMessageDialog(this, "Au fost înlocuite " + count + " apariții!", "Înlocuire finalizată", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Subșirul nu a fost găsit pentru înlocuire!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Выделение (подсветка желтым цветом) всех вхождений подстроки в тексте.
     */
    private void highlightAll() {
        JTextPane pane = getActiveTextPane();
        if (pane == null) return;

        Highlighter highlighter = pane.getHighlighter();
        highlighter.removeAllHighlights();

        String target = findField.getText();
        if (target.isEmpty()) return;

        String text = pane.getText();
        boolean matchCase = matchCaseCheck.isSelected();
        String searchText = matchCase ? text : text.toLowerCase();
        String searchTarget = matchCase ? target : target.toLowerCase();

        int index = 0;
        int count = 0;
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 243, 130)); // Желтая подсветка

        try {
            while ((index = searchText.indexOf(searchTarget, index)) != -1) {
                highlighter.addHighlight(index, index + target.length(), painter);
                index += target.length();
                count++;
            }
            JOptionPane.showMessageDialog(this, "Au fost evidențiate " + count + " apariții!", "Evidențiere", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

