import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Сервис получения курсов валют в Молдове с сайта curs.md (официальный курс BNM + рыночный курс банков и обменников)
// Лабораторная работа 5 - пункт a
public class CurrencyService {

    public enum Trend {
        UP,    // курс вырос 🟢
        DOWN,  // курс упал 🔴
        STABLE // курс стабилен ⚪
    }

    public static class CurrencyData {
        private final double eur;
        private final double usd;
        private final double ron;
        private final double eurBuy;
        private final double eurSell;
        private final double usdBuy;
        private final double usdSell;
        private final double ronBuy;
        private final double ronSell;
        private final Trend eurTrend;
        private final Trend usdTrend;
        private final boolean isOnline;
        private final String source;
        private final String time;

        public CurrencyData(double eur, double usd, double ron,
                            double eurBuy, double eurSell,
                            double usdBuy, double usdSell,
                            double ronBuy, double ronSell,
                            Trend eurTrend, Trend usdTrend, boolean online, String source) {
            this.eur = eur;
            this.usd = usd;
            this.ron = ron;
            this.eurBuy = eurBuy;
            this.eurSell = eurSell;
            this.usdBuy = usdBuy;
            this.usdSell = usdSell;
            this.ronBuy = ronBuy;
            this.ronSell = ronSell;
            this.eurTrend = eurTrend;
            this.usdTrend = usdTrend;
            this.isOnline = online;
            this.source = source;
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        // Конструктор совместимости
        public CurrencyData(double eur, double usd, Trend eurTrend, Trend usdTrend, boolean online) {
            this(eur, usd, 3.82, eur - 0.10, eur + 0.06, usd - 0.14, usd + 0.05, 3.78, 3.83, eurTrend, usdTrend, online, "curs.md");
        }

        public double getEur() { return eur; }
        public double getUsd() { return usd; }
        public double getRon() { return ron; }
        public double getEurBuy() { return eurBuy; }
        public double getEurSell() { return eurSell; }
        public double getUsdBuy() { return usdBuy; }
        public double getUsdSell() { return usdSell; }
        public double getRonBuy() { return ronBuy; }
        public double getRonSell() { return ronSell; }
        public Trend getEurTrend() { return eurTrend; }
        public Trend getUsdTrend() { return usdTrend; }
        public boolean isOnline() { return isOnline; }
        public String getSource() { return source; }
        public String getTime() { return time; }

        @Override
        public String toString() {
            return String.format("EUR: %.2f MDL (Piață: %.2f/%.2f), USD: %.2f MDL (Piață: %.2f/%.2f)",
                    eur, eurBuy, eurSell, usd, usdBuy, usdSell);
        }
    }

    private double lastEur = 20.12;
    private double lastUsd = 17.54;

    private static final String CURS_MD_URL = "https://www.curs.md/ro";

    public CurrencyData fetchRates() {
        try {
            URI uri = new URI(CURS_MD_URL);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) html.append(line).append("\n");
                reader.close();

                return parseCursMdHtml(html.toString(), true);
            }
        } catch (Exception e) {
            System.out.println("Не удалось загрузить данные с curs.md: " + e.getMessage());
        }

        return getFallbackRates();
    }

    public CurrencyData parseCursMdHtml(String html, boolean online) {
        // 1. Официальные курсы BNM
        double eurBnm = parseBnmRate(html, "EUR");
        double usdBnm = parseBnmRate(html, "USD");
        double ronBnm = parseBnmRate(html, "RON");

        // 2. Рыночные средние курсы банков и обменников (Bănci: cump. mediu / vanz. mediu)
        double[] eurMarket = parseMarketChart(html, "EUR_chart");
        double[] usdMarket = parseMarketChart(html, "USD_chart");
        double[] ronMarket = parseMarketChart(html, "RON_chart");

        if (eurBnm == 0.0 && eurMarket[0] > 0) eurBnm = eurMarket[0];
        if (usdBnm == 0.0 && usdMarket[0] > 0) usdBnm = usdMarket[0];
        if (ronBnm == 0.0 && ronMarket[0] > 0) ronBnm = ronMarket[0];

        if (eurBnm == 0.0) eurBnm = 20.12;
        if (usdBnm == 0.0) usdBnm = 17.54;
        if (ronBnm == 0.0) ronBnm = 3.82;

        double eurBuy = eurMarket[1] > 0 ? eurMarket[1] : (eurBnm - 0.10);
        double eurSell = eurMarket[2] > 0 ? eurMarket[2] : (eurBnm + 0.06);

        double usdBuy = usdMarket[1] > 0 ? usdMarket[1] : (usdBnm - 0.14);
        double usdSell = usdMarket[2] > 0 ? usdMarket[2] : (usdBnm + 0.05);

        double ronBuy = ronMarket[1] > 0 ? ronMarket[1] : (ronBnm - 0.04);
        double ronSell = ronMarket[2] > 0 ? ronMarket[2] : (ronBnm + 0.01);

        Trend eTrend = eurBnm > lastEur ? Trend.UP : (eurBnm < lastEur ? Trend.DOWN : Trend.STABLE);
        Trend uTrend = usdBnm > lastUsd ? Trend.UP : (usdBnm < lastUsd ? Trend.DOWN : Trend.STABLE);

        this.lastEur = eurBnm;
        this.lastUsd = usdBnm;

        return new CurrencyData(eurBnm, usdBnm, ronBnm, eurBuy, eurSell, usdBuy, usdSell, ronBuy, ronSell,
                eTrend, uTrend, online, "curs.md (BNM & Piață)");
    }

    private static double parseBnmRate(String html, String code) {
        try {
            int curIdx = html.indexOf("<td class=\"currency\">" + code + "</td>");
            if (curIdx != -1) {
                int rateIdx = html.indexOf("<td class=\"rate\">", curIdx);
                if (rateIdx != -1) {
                    int start = rateIdx + "<td class=\"rate\">".length();
                    int end = html.indexOf(" Lei", start);
                    if (end != -1 && end > start) {
                        String valStr = html.substring(start, end).trim().replace(",", ".");
                        return Double.parseDouble(valStr);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private static double[] parseMarketChart(String html, String chartId) {
        try {
            int markerIdx = html.indexOf("document.getElementById(\"" + chartId + "\"");
            if (markerIdx == -1) markerIdx = html.indexOf(chartId);
            if (markerIdx != -1) {
                int addRowsEnd = html.lastIndexOf("]]);", markerIdx);
                if (addRowsEnd != -1) {
                    int lastRowStart = html.lastIndexOf("[", addRowsEnd - 1);
                    int lastRowEnd = html.lastIndexOf("]", addRowsEnd);
                    if (lastRowStart != -1 && lastRowEnd > lastRowStart) {
                        String row = html.substring(lastRowStart + 1, lastRowEnd);
                        int dateEnd = row.indexOf(")");
                        if (dateEnd != -1) {
                            String numbersPart = row.substring(dateEnd + 1);
                            String[] rawParts = numbersPart.split(",");
                            List<Double> list = new ArrayList<>();
                            for (String p : rawParts) {
                                p = p.trim();
                                if (!p.isEmpty()) list.add(Double.parseDouble(p));
                            }
                            if (list.size() >= 3) {
                                return new double[]{list.get(0), list.get(1), list.get(2)};
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return new double[]{0.0, 0.0, 0.0};
    }

    public CurrencyData getFallbackRates() {
        return new CurrencyData(20.12, 17.54, 3.82, 20.02, 20.18, 17.40, 17.59, 3.78, 3.83,
                Trend.UP, Trend.UP, false, "curs.md (Offline)");
    }
}
