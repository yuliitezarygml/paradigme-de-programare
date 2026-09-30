import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Главное окно резидентного приложения (Meteo & Valută с определением публичного IP)
// Лабораторная работа 5
public class MainWindow extends JFrame {

    private final SettingsManager settingsManager;
    private final IpService ipService;
    private final WeatherService weatherService;
    private final CurrencyService currencyService;
    private final TrayManager trayManager;
    private final HotkeyManager hotkeyManager;

    private Timer refreshTimer;

    // UI элементы
    private JLabel statusBadge;
    private JLabel ipInfoLabel;

    private JLabel weatherCardTitle;
    private JLabel tempLabel;
    private JLabel conditionLabel;
    private JLabel windLabel;
    private JLabel weatherTimeLabel;

    private JLabel eurLabel;
    private JLabel usdLabel;
    private JLabel ronLabel;
    private JLabel currencyTimeLabel;

    private JCheckBox autoRefreshCheck;
    private JComboBox<String> intervalCombo;

    public MainWindow() {
        setTitle("Monitor Meteo & Valută (curs.md / IP Public) - Lab 5");
        setSize(740, 590);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.settingsManager = new SettingsManager();
        this.ipService = new IpService();
        this.weatherService = new WeatherService();
        this.currencyService = new CurrencyService();

        // 1. Инициализация системного трея
        this.trayManager = new TrayManager(new TrayManager.TrayListener() {
            @Override public void onOpenWindow() { showWindow(); }
            @Override public void onRefreshNow() { refreshData(); }
            @Override public void onIntervalChanged(int sec) { changeInterval(sec); }
            @Override public void onExit() { exitApp(); }
        });

        // 2. Инициализация горячей клавиши (Ctrl/Cmd + Shift + W)
        this.hotkeyManager = new HotkeyManager(this::toggleVisibility);

        // 3. Перехват клика по крестику закрытия - прячем в трей вместо закрытия
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });

        initUI();
        initTimer();

        // Первичная загрузка данных
        refreshData();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(new Color(245, 246, 248));

        // --- ВЕРХНИЙ ЗАГОЛОВОК С ИНФОРМАЦИЕЙ О ПУБЛИЧНОМ IP ---
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);

        JPanel headerTop = new JPanel(new BorderLayout());
        headerTop.setOpaque(false);

        JLabel title = new JLabel("📡 Monitor: Meteo & Curs Valutar");
        title.setFont(new Font("Arial", Font.BOLD, 17));
        title.setForeground(new Color(25, 30, 40));

        statusBadge = new JLabel(" 🟢 Online ");
        statusBadge.setFont(new Font("Arial", Font.BOLD, 12));
        statusBadge.setOpaque(true);
        statusBadge.setBackground(new Color(220, 245, 220));
        statusBadge.setForeground(new Color(20, 120, 20));
        statusBadge.setBorder(BorderFactory.createLineBorder(new Color(160, 210, 160), 1, true));

        headerTop.add(title, BorderLayout.WEST);
        headerTop.add(statusBadge, BorderLayout.EAST);

        // Строка с публичным IP адресом и провайдером
        ipInfoLabel = new JLabel("🌐 IP Public: Se identifică...");
        ipInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        ipInfoLabel.setForeground(new Color(80, 85, 95));

        header.add(headerTop, BorderLayout.NORTH);
        header.add(ipInfoLabel, BorderLayout.SOUTH);

        // --- ЦЕНТРАЛЬНЫЕ КАРТОЧКИ ---
        JPanel cardsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        cardsPanel.setOpaque(false);

        // Карточка 1: Погода
        JPanel weatherCard = createCard("⛅ Vremea locală (Open-Meteo API)");
        weatherCardTitle = (JLabel) weatherCard.getComponent(0);

        JPanel weatherContent = new JPanel(new GridLayout(2, 2, 8, 6));
        weatherContent.setOpaque(false);

        tempLabel = new JLabel("+0.0°C");
        tempLabel.setFont(new Font("Arial", Font.BOLD, 28));
        tempLabel.setForeground(new Color(26, 115, 232));

        conditionLabel = new JLabel("Stare: Se încarcă...");
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        windLabel = new JLabel("Vânt: 0 km/h");
        windLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        weatherTimeLabel = new JLabel("Ora: --:--:--");
        weatherTimeLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        weatherTimeLabel.setForeground(Color.GRAY);

        weatherContent.add(tempLabel);
        weatherContent.add(conditionLabel);
        weatherContent.add(windLabel);
        weatherContent.add(weatherTimeLabel);
        weatherCard.add(weatherContent, BorderLayout.CENTER);

        // Карточка 2: Курсы валют (curs.md)
        JPanel currencyCard = createCard("💶 Curs Valutar Moldova (curs.md — Oficial BNM & Piața Bancară)");
        JPanel currencyContent = new JPanel(new GridLayout(4, 1, 4, 5));
        currencyContent.setOpaque(false);

        eurLabel = new JLabel("💶 EUR: Oficial 0.00 MDL | Piață: Cump. 0.00 / Vânz. 0.00");
        eurLabel.setFont(new Font("Arial", Font.BOLD, 13));
        eurLabel.setForeground(new Color(35, 40, 50));

        usdLabel = new JLabel("💵 USD: Oficial 0.00 MDL | Piață: Cump. 0.00 / Vânz. 0.00");
        usdLabel.setFont(new Font("Arial", Font.BOLD, 13));
        usdLabel.setForeground(new Color(35, 40, 50));

        ronLabel = new JLabel("🪙 RON: Oficial 0.00 MDL | Piață: Cump. 0.00 / Vânz. 0.00");
        ronLabel.setFont(new Font("Arial", Font.BOLD, 13));
        ronLabel.setForeground(new Color(35, 40, 50));

        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setOpaque(false);

        JLabel sourceLbl = new JLabel("📊 Sursa: curs.md (Banca Națională & Bănci Comerciale)");
        sourceLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        sourceLbl.setForeground(Color.DARK_GRAY);

        currencyTimeLabel = new JLabel("Actualizat: --:--:--");
        currencyTimeLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        currencyTimeLabel.setForeground(Color.GRAY);

        subRow.add(sourceLbl, BorderLayout.WEST);
        subRow.add(currencyTimeLabel, BorderLayout.EAST);

        currencyContent.add(eurLabel);
        currencyContent.add(usdLabel);
        currencyContent.add(ronLabel);
        currencyContent.add(subRow);
        currencyCard.add(currencyContent, BorderLayout.CENTER);

        cardsPanel.add(weatherCard);
        cardsPanel.add(currencyCard);

        // --- НИЖНЯЯ ПАНЕЛЬ НАСТРОЕК ТАЙМЕРА И КНОПОК ---
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setOpaque(false);

        // Панель настроек авто-обновления
        JPanel settingsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        settingsBar.setBackground(Color.WHITE);
        settingsBar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 225), 1, true));

        autoRefreshCheck = new JCheckBox("Actualizare automată", settingsManager.isAutoRefresh());
        autoRefreshCheck.setFont(new Font("Arial", Font.PLAIN, 12));
        autoRefreshCheck.addActionListener(e -> {
            settingsManager.setAutoRefresh(autoRefreshCheck.isSelected());
            if (autoRefreshCheck.isSelected()) refreshTimer.start();
            else refreshTimer.stop();
        });

        JLabel intLbl = new JLabel("Interval:");
        intLbl.setFont(new Font("Arial", Font.PLAIN, 12));

        intervalCombo = new JComboBox<>(new String[]{"5 secunde", "15 secunde", "30 secunde", "60 secunde"});
        selectComboBySeconds(settingsManager.getRefreshIntervalSeconds());
        intervalCombo.addActionListener(e -> {
            int sec = getSecondsFromCombo();
            changeInterval(sec);
        });

        JButton btnRefreshNow = new JButton("🔄 Actualizează acum");
        btnRefreshNow.setFont(new Font("Arial", Font.BOLD, 12));
        btnRefreshNow.addActionListener(e -> refreshData());

        settingsBar.add(autoRefreshCheck);
        settingsBar.add(intLbl);
        settingsBar.add(intervalCombo);
        settingsBar.add(btnRefreshNow);

        // Кнопки сворачивания и горячих клавиш
        JPanel actionsBar = new JPanel(new BorderLayout());
        actionsBar.setOpaque(false);

        JLabel hotkeyLbl = new JLabel("⌨️ Hotkey: [Cmd/Ctrl + Shift + W] pentru a ascunde / afișa");
        hotkeyLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        hotkeyLbl.setForeground(new Color(100, 100, 110));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);

        JButton btnHide = new JButton("📥 Ascunde în Tray");
        btnHide.addActionListener(e -> hideToTray());

        JButton btnExit = new JButton("Ieșire");
        btnExit.addActionListener(e -> exitApp());

        btns.add(btnHide);
        btns.add(btnExit);

        actionsBar.add(hotkeyLbl, BorderLayout.WEST);
        actionsBar.add(btns, BorderLayout.EAST);

        bottomPanel.add(settingsBar, BorderLayout.NORTH);
        bottomPanel.add(actionsBar, BorderLayout.SOUTH);

        root.add(header, BorderLayout.NORTH);
        root.add(cardsPanel, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout(6, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 222, 228), 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 13));
        titleLbl.setForeground(new Color(70, 75, 85));
        card.add(titleLbl, BorderLayout.NORTH);
        return card;
    }

    private void initTimer() {
        int delayMs = settingsManager.getRefreshIntervalSeconds() * 1000;
        refreshTimer = new Timer(delayMs, e -> refreshData());
        if (settingsManager.isAutoRefresh()) {
            refreshTimer.start();
        }
    }

    // Обновление данных из интернета (в фоновом потоке)
    public void refreshData() {
        new Thread(() -> {
            // 1. Определяем публичный IP и город пользователя
            IpService.IpInfo ipInfo = ipService.fetchPublicIpInfo();

            // 2. Получаем точную погоду по координатам публичного IP
            WeatherService.WeatherData weather = weatherService.fetchWeather(
                ipInfo.getLat(),
                ipInfo.getLon(),
                ipInfo.getCity() + ", " + ipInfo.getCountry()
            );

            // 3. Получаем курсы валют
            CurrencyService.CurrencyData currency = currencyService.fetchRates();

            SwingUtilities.invokeLater(() -> {
                // Обновляем информацию о публичном IP
                String ispText = (ipInfo.getIsp() != null && !ipInfo.getIsp().isEmpty()) ? (" | ISP: " + ipInfo.getIsp()) : "";
                ipInfoLabel.setText(String.format("🌐 IP Public: %s (%s, %s)%s", ipInfo.getIp(), ipInfo.getCity(), ipInfo.getCountry(), ispText));

                // Обновляем заголовок карточки погоды с определенным городом
                weatherCardTitle.setText(String.format("⛅ Vremea: %s, %s (detectat prin IP)", ipInfo.getCity(), ipInfo.getCountry()));

                // Обновляем виджет погоды
                tempLabel.setText(String.format("%+.1f°C", weather.getTemperature()));
                conditionLabel.setText("Stare: " + weather.getConditionText());
                windLabel.setText(String.format("Vânt: %.1f km/h", weather.getWindSpeed()));
                weatherTimeLabel.setText("Ora: " + weather.getTime());

                // Обновляем виджет валют (curs.md: BNM + Piață)
                String eArrow = currency.getEurTrend() == CurrencyService.Trend.UP ? "🟢 ↗" : (currency.getEurTrend() == CurrencyService.Trend.DOWN ? "🔴 ↘" : "⚪");
                String uArrow = currency.getUsdTrend() == CurrencyService.Trend.UP ? "🟢 ↗" : (currency.getUsdTrend() == CurrencyService.Trend.DOWN ? "🔴 ↘" : "⚪");

                eurLabel.setText(String.format("💶 EUR: Oficial %.4f MDL %s | Piață: Cump. %.2f / Vânz. %.2f MDL",
                        currency.getEur(), eArrow, currency.getEurBuy(), currency.getEurSell()));
                usdLabel.setText(String.format("💵 USD: Oficial %.4f MDL %s | Piață: Cump. %.2f / Vânz. %.2f MDL",
                        currency.getUsd(), uArrow, currency.getUsdBuy(), currency.getUsdSell()));
                ronLabel.setText(String.format("🪙 RON: Oficial %.4f MDL | Piață: Cump. %.2f / Vânz. %.2f MDL",
                        currency.getRon(), currency.getRonBuy(), currency.getRonSell()));
                currencyTimeLabel.setText("curs.md: " + currency.getTime());

                // Обновляем статус сети
                boolean isOnline = ipInfo.isOnline() && weather.isOnline();
                if (isOnline) {
                    statusBadge.setText(" 🟢 Online ");
                    statusBadge.setBackground(new Color(220, 245, 220));
                    statusBadge.setForeground(new Color(20, 120, 20));
                } else {
                    statusBadge.setText(" 🟡 Offline (Local) ");
                    statusBadge.setBackground(new Color(255, 245, 210));
                    statusBadge.setForeground(new Color(150, 110, 20));
                }

                // Обновляем иконку и подсказку в System Tray
                trayManager.updateTray(weather, currency);
            });
        }).start();
    }

    public void changeInterval(int seconds) {
        settingsManager.setRefreshIntervalSeconds(seconds);
        selectComboBySeconds(seconds);
        if (refreshTimer != null) {
            refreshTimer.setDelay(seconds * 1000);
            refreshTimer.restart();
        }
    }

    private int getSecondsFromCombo() {
        int idx = intervalCombo.getSelectedIndex();
        switch (idx) {
            case 0: return 5;
            case 1: return 15;
            case 2: return 30;
            case 3: return 60;
            default: return 15;
        }
    }

    private void selectComboBySeconds(int seconds) {
        if (seconds <= 5) intervalCombo.setSelectedIndex(0);
        else if (seconds <= 15) intervalCombo.setSelectedIndex(1);
        else if (seconds <= 30) intervalCombo.setSelectedIndex(2);
        else intervalCombo.setSelectedIndex(3);
    }

    public void hideToTray() {
        setVisible(false);
        trayManager.showNotification("Meteo & Valută", "Aplicația rulează în fundal în System Tray.");
    }

    public void showWindow() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        requestFocus();
    }

    public void toggleVisibility() {
        if (isVisible()) hideToTray();
        else showWindow();
    }

    public void exitApp() {
        if (refreshTimer != null) refreshTimer.stop();
        System.exit(0);
    }
}
