import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Сервис для получения данных о погоде в реальном времени через онлайн API (Open-Meteo)
// Лабораторная работа 5 - пункт a
public class WeatherService {

    public static class WeatherData {
        private final String city;
        private final double temperature;
        private final double windSpeed;
        private final int weatherCode;
        private final String conditionText;
        private final String iconType; // SUN, CLOUD, RAIN, SNOW
        private final boolean isOnline;
        private final String time;

        public WeatherData(String city, double temp, double wind, int code, String condition, String icon, boolean online) {
            this.city = city;
            this.temperature = temp;
            this.windSpeed = wind;
            this.weatherCode = code;
            this.conditionText = condition;
            this.iconType = icon;
            this.isOnline = online;
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        public String getCity() { return city; }
        public double getTemperature() { return temperature; }
        public double getWindSpeed() { return windSpeed; }
        public int getWeatherCode() { return weatherCode; }
        public String getConditionText() { return conditionText; }
        public String getIconType() { return iconType; }
        public boolean isOnline() { return isOnline; }
        public String getTime() { return time; }

        @Override
        public String toString() {
            return String.format("%s: %+.1f°C, %s (Vânt: %.1f km/h)", city, temperature, conditionText, windSpeed);
        }
    }

    public WeatherData fetchWeather(double lat, double lon, String city) {
        String urlString = String.format(
            java.util.Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
            lat, lon
        );

        try {
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
                reader.close();

                return parseWeatherJson(json.toString(), city, true);
            }
        } catch (Exception e) {
            System.out.println("Нет связи с Open-Meteo, используется офлайн-режим: " + e.getMessage());
        }

        return getFallbackWeather(city);
    }

    // Парсинг блока "current_weather" в JSON
    public static WeatherData parseWeatherJson(String json, String city, boolean online) {
        // Ищем именно блок current_weather, чтобы не захватить current_weather_units
        int cwIdx = json.indexOf("\"current_weather\":");
        String targetBlock = (cwIdx != -1) ? json.substring(cwIdx) : json;

        double temp = extractNumber(targetBlock, "temperature");
        double wind = extractNumber(targetBlock, "windspeed");
        int code = (int) extractNumber(targetBlock, "weathercode");

        String condition;
        String iconType;

        if (code == 0) {
            condition = "Însorit (Солнечно)";
            iconType = "SUN";
        } else if (code <= 3) {
            condition = "Parțial înnorat (Облачно)";
            iconType = "CLOUD";
        } else if (code >= 51 && code <= 67) {
            condition = "Ploaie (Дождь)";
            iconType = "RAIN";
        } else if (code >= 71 && code <= 77) {
            condition = "Ninsoare (Снег)";
            iconType = "SNOW";
        } else {
            condition = "Variabil (Переменно)";
            iconType = "CLOUD";
        }

        if (city == null || city.isEmpty()) city = "Chișinău";
        return new WeatherData(city, temp, wind, code, condition, iconType, online);
    }

    private static double extractNumber(String text, String key) {
        try {
            int idx = text.indexOf("\"" + key + "\":");
            if (idx == -1) return 0.0;
            int start = idx + key.length() + 3;
            while (start < text.length() && (text.charAt(start) == ' ' || text.charAt(start) == ':')) start++;
            int end = start;
            while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.' || text.charAt(end) == '-')) {
                end++;
            }
            if (end > start) {
                return Double.parseDouble(text.substring(start, end));
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    // Перегрузка для совместимости
    public static WeatherData parseWeatherJson(String json, boolean online) {
        return parseWeatherJson(json, "Chișinău", online);
    }

    public static WeatherData getFallbackWeather(String city) {
        if (city == null || city.isEmpty()) city = "Chișinău";
        return new WeatherData(city, 20.4, 7.8, 1, "Parțial înnorat (Offline)", "CLOUD", false);
    }

    public static WeatherData getFallbackWeather() {
        return getFallbackWeather("Chișinău");
    }
}
