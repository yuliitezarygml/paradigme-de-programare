import java.awt.Graphics;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Прячет стандартные заголовки JTabbedPane.
 * Свою полосу вкладок рисует TextEditor, потому что стандартная
 * на macOS сжимается и вкладки наезжают друг на друга.
 */
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

    @Override
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        return 0;
    }

    @Override
    protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth) {
        return 0;
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
    }
}
