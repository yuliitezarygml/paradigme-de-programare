import java.io.File;
import java.util.List;

// Автоматические тесты для Лабораторной работы 4
public class TestBrowser {

    private static int passed = 0;
    private static int total = 0;

    private static void assertTrue(String testName, boolean condition) {
        total++;
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Запуск тестов Лабораторной работы 4 (Интернет-Браузер)");
        System.out.println("==================================================");

        testFavoritesManager();
        testHistoryManager();
        testUrlResolution();

        System.out.println("==================================================");
        System.out.println("Результат тестов: " + passed + " / " + total + " пройдено.");
        if (passed == total) {
            System.out.println("ВСЕ ТЕСТЫ УСПЕШНО ПРОЙДЕНЫ!");
        } else {
            System.err.println("НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОШЛИ!");
            System.exit(1);
        }
    }

    // Тестирование FavoritesManager (пункт b)
    private static void testFavoritesManager() {
        System.out.println("\n1. Тестирование менеджера закладок (FavoritesManager):");
        File testFile = new File("favorites.txt");
        if (testFile.exists()) testFile.delete();

        FavoritesManager fm = new FavoritesManager();
        assertTrue("Список не null", fm.getFavorites() != null);

        // Проверяем добавление
        String url1 = "https://www.google.com";
        String url2 = "https://www.youtube.com";
        fm.addFavorite(url1);
        fm.addFavorite(url2);

        assertTrue("Google есть в закладках", fm.isFavorite(url1));
        assertTrue("YouTube есть в закладках", fm.isFavorite(url2));
        assertTrue("Несуществующий сайт отсутствует", !fm.isFavorite("https://notfound.org"));

        // Проверяем сохранение и чтение из файла
        FavoritesManager fm2 = new FavoritesManager();
        assertTrue("Закладки сохранились и загрузились из файла", fm2.isFavorite(url1));

        // Проверяем удаление
        fm.removeFavorite(url1);
        assertTrue("Сайт удален из закладок", !fm.isFavorite(url1));
    }

    // Тестирование HistoryManager (пункт c)
    private static void testHistoryManager() {
        System.out.println("\n2. Тестирование истории посещений (HistoryManager):");
        HistoryManager hm = new HistoryManager();
        assertTrue("История изначально пуста", hm.getHistory().isEmpty());

        hm.addEntry("https://example.com", "Example Domain");
        hm.addEntry("https://youtube.com", "YouTube");

        List<HistoryManager.Entry> list = hm.getHistory();
        assertTrue("В истории 2 записи", list.size() == 2);
        assertTrue("URL первой записи верен", "https://example.com".equals(list.get(0).getUrl()));
        assertTrue("Заголовок первой записи верен", "Example Domain".equals(list.get(0).getTitle()));
        assertTrue("Время записи сформировано", list.get(0).getTime() != null && !list.get(0).getTime().isEmpty());

        // Очистка истории
        hm.clear();
        assertTrue("История успешно очищена", hm.getHistory().isEmpty());
    }

    // Тестирование преобразования URL / поисковых запросов (пункт f)
    private static void testUrlResolution() {
        System.out.println("\n3. Тестирование распознавания URL и поисковых запросов:");

        String empty = BrowserTab.resolveUrlOrSearch("");
        assertTrue("Пустой ввод -> about:blank", "about:blank".equals(empty));

        String domain = BrowserTab.resolveUrlOrSearch("example.com");
        assertTrue("example.com -> https://example.com", "https://example.com".equals(domain));

        String full = BrowserTab.resolveUrlOrSearch("https://youtube.com");
        assertTrue("https://youtube.com -> без изменений", "https://youtube.com".equals(full));

        String query = BrowserTab.resolveUrlOrSearch("купить кофе кишинев");
        assertTrue("Поисковый запрос направляется в поисковик", query.contains("google.com/search?q=") || query.contains("duckduckgo"));
    }
}
