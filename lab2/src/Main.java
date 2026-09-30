import javax.swing.*;

/**
 * Главный класс запуска приложения.
 * Лабораторная работа №2: Текстовый редактор (ООП на Java).
 */
public class Main {
    public static void main(String[] args) {
        // Устанавливаем системный стиль окон
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // Запуск интерфейса
        SwingUtilities.invokeLater(() -> {
            TextEditor editor = new TextEditor();
            editor.setVisible(true);
        });
    }
}

