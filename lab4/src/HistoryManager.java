import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Менеджер истории посещенных страниц
// Лабораторная работа 4 - пункт c (History)
public class HistoryManager {

    // Внутренний класс для одной записи в истории
    public static class Entry {
        private final String url;
        private final String title;
        private final String time;

        public Entry(String url, String title) {
            this.url = url;
            this.title = (title != null && !title.isEmpty()) ? title : url;
            this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getTime() { return time; }

        @Override
        public String toString() {
            return "[" + time + "] " + title + " (" + url + ")";
        }
    }

    private final List<Entry> history = new ArrayList<>();

    // Добавить посещенный сайт
    public void addEntry(String url, String title) {
        if (url == null || url.trim().isEmpty() || "about:blank".equalsIgnoreCase(url)) {
            return;
        }
        history.add(new Entry(url, title));
    }

    public List<Entry> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }
}
