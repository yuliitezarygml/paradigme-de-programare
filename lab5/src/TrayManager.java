import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

// Управление системным треем (System Tray) и динамическими иконками
// Лабораторная работа 5 - пункты b и d
public class TrayManager {

    public interface TrayListener {
        void onOpenWindow();
        void onRefreshNow();
        void onIntervalChanged(int seconds);
        void onExit();
    }

    private final TrayListener listener;
    private TrayIcon trayIcon;
    private boolean supported = false;

    public TrayManager(TrayListener listener) {
        this.listener = listener;
        initTray();
    }

    private void initTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System Tray не поддерживается на данной операционной системе!");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Создаем стартовую иконку (солнце)
            Image defaultImage = createWeatherIcon("SUN", "+21°C");
            PopupMenu popup = createPopupMenu();

            trayIcon = new TrayIcon(defaultImage, "Meteo & Valută (Lab 5)", popup);
            trayIcon.setImageAutoSize(true);

            // Клик по иконке в трее открывает окно
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 1 && listener != null) {
                        listener.onOpenWindow();
                    }
                }
            });

            tray.add(trayIcon);
            this.supported = true;
            System.out.println("Приложение успешно зарегистрировано в System Tray!");
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении в System Tray: " + e.getMessage());
        }
    }

    // Создание контекстного меню трея
    private PopupMenu createPopupMenu() {
        PopupMenu menu = new PopupMenu();

        MenuItem itemOpen = new MenuItem("Deschide fereastra");
        itemOpen.addActionListener(e -> { if (listener != null) listener.onOpenWindow(); });

        MenuItem itemRefresh = new MenuItem("Actualizează acum");
        itemRefresh.addActionListener(e -> { if (listener != null) listener.onRefreshNow(); });

        Menu intervalMenu = new Menu("Interval actualizare");
        int[] intervals = {5, 15, 30, 60};
        for (int sec : intervals) {
            MenuItem item = new MenuItem(sec + " secunde");
            item.addActionListener(e -> { if (listener != null) listener.onIntervalChanged(sec); });
            intervalMenu.add(item);
        }

        MenuItem itemExit = new MenuItem("Ieșire");
        itemExit.addActionListener(e -> { if (listener != null) listener.onExit(); });

        menu.add(itemOpen);
        menu.add(itemRefresh);
        menu.add(intervalMenu);
        menu.addSeparator();
        menu.add(itemExit);

        return menu;
    }

    // Динамическая генерация иконки для трея на основе погоды (требование пункта d)
    public static Image createWeatherIcon(String type, String tempText) {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if ("RAIN".equalsIgnoreCase(type)) {
            // Дождь: темная тучка и синие капли
            g2.setColor(new Color(110, 130, 150));
            g2.fillOval(4, 6, 16, 12);
            g2.fillOval(12, 4, 16, 14);
            g2.setColor(new Color(30, 144, 255));
            g2.drawLine(8, 22, 6, 28);
            g2.drawLine(16, 22, 14, 28);
            g2.drawLine(24, 22, 22, 28);
        } else if ("CLOUD".equalsIgnoreCase(type)) {
            // Облачно: светлая тучка
            g2.setColor(new Color(176, 196, 222));
            g2.fillOval(4, 8, 16, 14);
            g2.fillOval(12, 5, 16, 17);
        } else {
            // Солнце: желтый круг с лучами
            g2.setColor(new Color(255, 185, 0));
            g2.fillOval(7, 7, 18, 18);
            g2.setColor(new Color(255, 140, 0));
            g2.drawOval(7, 7, 18, 18);
            // Лучи солнца
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(16, 2, 16, 5);
            g2.drawLine(16, 27, 16, 30);
            g2.drawLine(2, 16, 5, 16);
            g2.drawLine(27, 16, 30, 16);
        }

        g2.dispose();
        return img;
    }

    // Обновление иконки и всплывающей подсказки в трее
    public void updateTray(WeatherService.WeatherData weather, CurrencyService.CurrencyData currency) {
        if (!supported || trayIcon == null) return;

        Image newIcon = createWeatherIcon(weather.getIconType(), String.format("%.0f°", weather.getTemperature()));
        trayIcon.setImage(newIcon);

        String tooltip = String.format("Meteo: %.1f°C (%s) | EUR: %.2f | USD: %.2f",
            weather.getTemperature(), weather.getConditionText(), currency.getEur(), currency.getUsd());
        trayIcon.setToolTip(tooltip);
    }

    // Показать системное уведомление при сворачивании в трей
    public void showNotification(String title, String message) {
        if (supported && trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public boolean isSupported() {
        return supported;
    }
}
