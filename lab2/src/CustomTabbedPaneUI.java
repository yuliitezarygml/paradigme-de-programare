import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * CustomTabbedPaneUI oferă un aspect curat, modern și aliniat la STÂNGA pentru file (tabs).
 *
 * Rezolvă complet problema trunchierii titlurilor ("...") prin calcularea
 * lățimii reale a componentei de antet (titlu + butonul de închidere).
 */
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        // Spațiere confortabilă în interiorul fiecărei file
        return new Insets(6, 10, 6, 10);
    }

    @Override
    protected Insets getTabAreaInsets(int tabPlacement) {
        // Tab-urile încep din stânga, cu o margine de 8px
        return new Insets(6, 10, 0, 10);
    }

    /**
     * Calculează lățimea reală a filei luând în considerare componenta de antet
     * (titlul documentului + butonul '×' de închidere).
     */
    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        if (tabPane != null && tabIndex < tabPane.getTabCount()) {
            Component c = tabPane.getTabComponentAt(tabIndex);
            if (c != null) {
                return c.getPreferredSize().width + 20;
            }
        }
        return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 25;
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return Math.max(super.calculateTabHeight(tabPlacement, tabIndex, fontHeight), 32);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            // Fila activă (selectată) are fundal alb curat
            g2.setColor(AppStyles.tabSelectedBackground);
            g2.fillRoundRect(x, y + 1, w, h + 4, 8, 8);
        } else {
            // Filele inactive au fundal gri deschis
            g2.setColor(AppStyles.tabBackground);
            g2.fillRoundRect(x + 1, y + 3, w - 2, h, 6, 6);
        }
        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color borderColor = new Color(0xCF, 0xD8, 0xDC);
        g2.setColor(borderColor);

        if (isSelected) {
            // Contur rotunjit pentru tab-ul activ
            g2.drawRoundRect(x, y + 1, w - 1, h + 4, 8, 8);
            // Ștergem linia de jos pentru a se îmbina cu zona de editare
            g2.setColor(AppStyles.tabSelectedBackground);
            g2.drawLine(x + 1, y + h, x + w - 2, y + h);
        } else {
            g2.drawRoundRect(x + 1, y + 3, w - 3, h - 1, 6, 6);
        }
        g2.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // Linie subtilă de separare între bara de tab-uri și document
        g.setColor(new Color(0xCF, 0xD8, 0xDC));
        g.drawLine(0, 0, tabPane.getWidth(), 0);
    }
}
