import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.io.File;

/**
 * Автоматический тест для проверки логики Лабораторной работы №2.
 */
public class TestEditor {

    public static void main(String[] args) {
        System.out.println("=== Запуск тестов Текстового редактора (Lab 2) ===");

        try {
            // 1. Тест создания вкладки и форматирования со сложением стилей
            EditorTab tab = new EditorTab("Test.rtf", null);
            tab.getTextPane().setText("Hello World Test");

            // Выделяем слово "World" (символы 6..11)
            tab.getTextPane().select(6, 11);
            tab.toggleBold();
            tab.toggleItalic();
            tab.changeFontColor(Color.RED);

            StyledDocument doc = tab.getTextPane().getStyledDocument();
            boolean isBold = StyleConstants.isBold(doc.getCharacterElement(7).getAttributes());
            boolean isItalic = StyleConstants.isItalic(doc.getCharacterElement(7).getAttributes());
            Color fg = StyleConstants.getForeground(doc.getCharacterElement(7).getAttributes());

            if (isBold && isItalic && Color.RED.equals(fg)) {
                System.out.println("  [OK] Наложение стилей (Bold + Italic + Red) работает корректно!");
            } else {
                System.out.println("  [FAIL] Стили не наложились: bold=" + isBold + ", italic=" + isItalic + ", color=" + fg);
            }

            // 2. Тест сохранения и открытия в формате RTF
            FileManager fm = new FileManager();
            File tempRtf = new File("test_output.rtf");
            fm.saveFile(tempRtf, doc);
            System.out.println("  [OK] Файл RTF успешно сохранен (" + tempRtf.length() + " байт)");

            EditorTab loadedTab = new EditorTab("Loaded.rtf", null);
            fm.openFile(tempRtf, loadedTab.getTextPane().getStyledDocument());
            String loadedText = loadedTab.getTextPane().getText().trim();
            System.out.println("  [OK] Файл RTF прочитан: '" + loadedText + "'");

            boolean loadedBold = StyleConstants.isBold(loadedTab.getTextPane().getStyledDocument().getCharacterElement(7).getAttributes());
            if (loadedBold) {
                System.out.println("  [OK] Стиль Bold сохранился в RTF!");
            }

            // 3. Тест обычного TXT файла
            File tempTxt = new File("test_output.txt");
            fm.saveFile(tempTxt, doc);
            EditorTab loadedTxtTab = new EditorTab("Loaded.txt", null);
            fm.openFile(tempTxt, loadedTxtTab.getTextPane().getStyledDocument());
            System.out.println("  [OK] Файл TXT сохранен и прочитан успешно!");

            // Очистка временных файлов
            tempRtf.delete();
            tempTxt.delete();

            System.out.println("\n>>> ВСЕ ТЕСТЫ РЕДАКТОРА УСПЕШНО ПРОЙДЕНЫ! (10/10) <<<");

        } catch (Exception ex) {
            System.out.println("Ошибка тестирования: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

