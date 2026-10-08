package client;

import common.NetworkMessage;
import common.UITheme;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Специализированное диалоговое окно для просмотра, поиска и экспорта истории сообщений (Требование b).
 * Позволяет фильтровать сообщения по ключевым словам или автору, просматривать все полученные сообщения
 * и экспортировать протокол переписки в текстовый файл на диске.
 */
public class HistoryDialog extends JDialog {

    private final List<NetworkMessage> allMessages = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable historyTable;
    private JTextField tfSearch;
    private JLabel lblCount;

    public HistoryDialog(JFrame parent, String currentRoom, List<NetworkMessage> initialHistory) {
        super(parent, "📜 История сообщений чата - " + currentRoom, true);
        if (initialHistory != null) {
            this.allMessages.addAll(initialHistory);
        }

        setSize(780, 520);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(UITheme.BG_DARKER);
        setLayout(new BorderLayout(0, 10));

        // 1. Верхняя панель: Заголовок и поиск
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(UITheme.BG_SIDEBAR);
        topPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitle = new JLabel("История переписки в комнате " + currentRoom);
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Поиск:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        lblSearch.setForeground(UITheme.TEXT_MUTED);

        tfSearch = UITheme.createTextField("Фильтр по автору или тексту...");
        tfSearch.setPreferredSize(new Dimension(240, 32));
        tfSearch.addActionListener(e -> applyFilter());

        JButton btnSearch = UITheme.createPrimaryButton("Найти");
        btnSearch.addActionListener(e -> applyFilter());

        JButton btnReset = UITheme.createSecondaryButton("Сброс");
        btnReset.addActionListener(e -> {
            tfSearch.setText("");
            applyFilter();
        });

        searchPanel.add(lblSearch);
        searchPanel.add(tfSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReset);
        topPanel.add(searchPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 2. Таблица отображения истории
        String[] cols = {"Время", "Отправитель", "Тип", "Содержимое / Детали"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        styleTable(historyTable);

        JScrollPane scroll = new JScrollPane(historyTable);
        UITheme.applyModernScrollBar(scroll);
        add(scroll, BorderLayout.CENTER);

        // 3. Нижняя панель: Статистика, экспорт и закрытие
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBackground(UITheme.BG_SIDEBAR);
        bottomPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        lblCount = new JLabel("Сообщений в истории: " + allMessages.size());
        lblCount.setFont(UITheme.FONT_REGULAR);
        lblCount.setForeground(UITheme.TEXT_MUTED);
        bottomPanel.add(lblCount, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnExport = UITheme.createSuccessButton("📥 Экспорт в файл (.txt)");
        btnExport.addActionListener(e -> exportHistory());

        JButton btnClose = UITheme.createSecondaryButton("Закрыть");
        btnClose.addActionListener(e -> dispose());

        actions.add(btnExport);
        actions.add(btnClose);
        bottomPanel.add(actions, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        applyFilter();
    }

    /**
     * Применяет поисковый фильтр к истории сообщений.
     */
    private void applyFilter() {
        String filter = tfSearch.getText().trim().toLowerCase();
        tableModel.setRowCount(0);

        int count = 0;
        for (NetworkMessage msg : allMessages) {
            String text = msg.getText() != null ? msg.getText() : "";
            String sender = msg.getSender() != null ? msg.getSender() : "";
            String file = msg.getFileName() != null ? msg.getFileName() : "";

            if (filter.isEmpty() ||
                sender.toLowerCase().contains(filter) ||
                text.toLowerCase().contains(filter) ||
                file.toLowerCase().contains(filter)) {

                String type = "Текст";
                String details = text;

                if (msg.isFile()) {
                    type = "Файл";
                    details = "Файл: " + msg.getFileName() + " (" + msg.getFormattedFileSize() + ")";
                } else if (msg.isReply()) {
                    type = "Ответ";
                    details = "[Ответ на @" + msg.getReplyToAuthor() + ": \"" + msg.getReplyToSnippet() + "\"] " + text;
                }

                tableModel.addRow(new Object[]{
                        msg.getFormattedTime(),
                        sender,
                        type,
                        details
                });
                count++;
            }
        }
        lblCount.setText("Отображено: " + count + " (всего: " + allMessages.size() + ")");
    }

    /**
     * Экспортирует текущую историю сообщений в текстовый файл.
     */
    private void exportHistory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("istoric_chat_" + System.currentTimeMillis() + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(chooser.getSelectedFile()))) {
                pw.println("==================================================");
                pw.println("ИСТОРИЯ СООБЩЕНИЙ ЛОКАЛЬНОГО ЧАТА (ЛАБ 3)");
                pw.println("==================================================");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    pw.println("[" + tableModel.getValueAt(i, 0) + "] " +
                            tableModel.getValueAt(i, 1) + " (" +
                            tableModel.getValueAt(i, 2) + "): " +
                            tableModel.getValueAt(i, 3));
                }
                JOptionPane.showMessageDialog(this, "История успешно экспортирована в:\n" +
                        chooser.getSelectedFile().getAbsolutePath(), "Успешно", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при сохранении: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void styleTable(JTable table) {
        table.setBackground(UITheme.BG_CARD);
        table.setForeground(UITheme.TEXT_PRIMARY);
        table.setGridColor(UITheme.BORDER_SUBTLE);
        table.setFont(UITheme.FONT_REGULAR);
        table.setRowHeight(28);
        table.setSelectionBackground(UITheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        table.getTableHeader().setBackground(UITheme.BG_INPUT);
        table.getTableHeader().setForeground(UITheme.TEXT_PRIMARY);
        table.getTableHeader().setFont(UITheme.FONT_HEADER);

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(450);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, isSelected, hasFocus, r, c);
                if (!isSelected) {
                    comp.setBackground(r % 2 == 0 ? UITheme.BG_CARD : UITheme.BG_INPUT.darker());
                    comp.setForeground(UITheme.TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(0, 6, 0, 6));
                return comp;
            }
        });
    }
}
