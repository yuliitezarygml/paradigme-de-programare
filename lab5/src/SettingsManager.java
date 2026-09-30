import java.io.*;
import java.util.Properties;

// Менеджер настроек приложения (сохраняет интервал обновления и параметры в settings.txt)
// Лабораторная работа 5 - пункт c
public class SettingsManager {

    private final File file;
    private final Properties props;

    // Значения по умолчанию
    private int refreshIntervalSeconds = 15;
    private boolean autoRefresh = true;

    public SettingsManager() {
        this.file = new File("settings.txt");
        this.props = new Properties();
        load();
    }

    // Загрузка настроек из файла
    public void load() {
        if (!file.exists()) {
            save(); // создаем файл с настройками по умолчанию
            return;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            String intervalStr = props.getProperty("refreshIntervalSeconds", "15");
            this.refreshIntervalSeconds = Integer.parseInt(intervalStr);
            this.autoRefresh = Boolean.parseBoolean(props.getProperty("autoRefresh", "true"));
        } catch (Exception e) {
            System.out.println("Ошибка при чтении настроек: " + e.getMessage());
        }
    }

    // Сохранение настроек в файл
    public void save() {
        props.setProperty("refreshIntervalSeconds", String.valueOf(refreshIntervalSeconds));
        props.setProperty("autoRefresh", String.valueOf(autoRefresh));

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Setari Aplicatie Rezident (Lab 5)");
        } catch (Exception e) {
            System.out.println("Ошибка при сохранении настроек: " + e.getMessage());
        }
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int seconds) {
        if (seconds < 2) seconds = 2; // минимум 2 секунды
        this.refreshIntervalSeconds = seconds;
        save();
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
        save();
    }
}
