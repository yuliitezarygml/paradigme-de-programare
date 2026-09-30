import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

// Сервис определения публичного IP-адреса и геолокации (город, страна, провайдер)
public class IpService {

    public static class IpInfo {
        private final String ip;
        private final String city;
        private final String country;
        private final String isp;
        private final double lat;
        private final double lon;
        private final boolean isOnline;

        public IpInfo(String ip, String city, String country, String isp, double lat, double lon, boolean online) {
            this.ip = ip;
            this.city = city;
            this.country = country;
            this.isp = isp;
            this.lat = lat;
            this.lon = lon;
            this.isOnline = online;
        }

        public String getIp() { return ip; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public String getIsp() { return isp; }
        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public boolean isOnline() { return isOnline; }

        @Override
        public String toString() {
            return String.format("%s (%s, %s) [%s]", ip, city, country, isp);
        }
    }

    private static final String IP_API_URL = "http://ip-api.com/json";

    public IpInfo fetchPublicIpInfo() {
        try {
            URI uri = new URI(IP_API_URL);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                return parseIpJson(json.toString(), true);
            }
        } catch (Exception e) {
            System.out.println("Не удалось получить публичный IP: " + e.getMessage());
        }

        return getFallbackIp();
    }

    public static IpInfo parseIpJson(String json, boolean online) {
        String ip = extractString(json, "\"query\":");
        if (ip.isEmpty()) ip = extractString(json, "\"ip\":");
        String city = extractString(json, "\"city\":");
        String country = extractString(json, "\"country\":");
        String isp = extractString(json, "\"isp\":");
        double lat = extractNumber(json, "\"lat\":");
        double lon = extractNumber(json, "\"lon\":");

        if (ip.isEmpty()) ip = "93.116.117.194";
        if (city.isEmpty()) city = "Chișinău";
        if (country.isEmpty()) country = "Moldova";
        if (lat == 0.0) lat = 47.01;
        if (lon == 0.0) lon = 28.86;

        return new IpInfo(ip, city, country, isp, lat, lon, online);
    }

    private static String extractString(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return "";
            int start = json.indexOf("\"", idx + key.length()) + 1;
            int end = json.indexOf("\"", start);
            if (start > 0 && end > start) {
                return json.substring(start, end);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static double extractNumber(String json, String key) {
        try {
            int idx = json.indexOf(key);
            if (idx == -1) return 0.0;
            int start = idx + key.length();
            while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == ':')) start++;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-')) {
                end++;
            }
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception ignored) {}
        return 0.0;
    }

    public static IpInfo getFallbackIp() {
        return new IpInfo("93.116.117.194", "Chișinău", "Moldova", "Moldtelecom", 47.0042, 28.8574, false);
    }
}
