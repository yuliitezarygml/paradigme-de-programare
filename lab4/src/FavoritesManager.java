import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Менеджер закладок (сохраняет избранные сайты в файл favorites.txt)
// Лабораторная работа 4 - пункт b (Favorites)
public class FavoritesManager {

    private final File file;
    private final List<String> favorites;

    public FavoritesManager() {
        this.file = new File("favorites.txt");
        this.favorites = new ArrayList<>();
        load();
    }

    // Добавить сайт в закладки
    public boolean addFavorite(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String clean = url.trim();
        if (!favorites.contains(clean)) {
            favorites.add(clean);
            System.out.println("Добавлено в закладки: " + clean);
            save();
            return true;
        }
        return false;
    }

    // Удалить сайт из закладок
    public boolean removeFavorite(String url) {
        boolean removed = favorites.remove(url);
        if (removed) {
            System.out.println("Удалено из закладок: " + url);
            save();
        }
        return removed;
    }

    // Проверка, добавлен ли сайт
    public boolean isFavorite(String url) {
        if (url == null) return false;
        return favorites.contains(url.trim());
    }

    public List<String> getFavorites() {
        return new ArrayList<>(favorites);
    }

    // Загрузить закладки из файла
    public void load() {
        favorites.clear();
        if (!file.exists()) {
            // Если файла еще нет, добавим несколько популярных сайтов по умолчанию
            favorites.add("https://www.google.com");
            favorites.add("https://www.youtube.com");
            favorites.add("https://en.wikipedia.org");
            save();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !favorites.contains(line)) {
                    favorites.add(line);
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка при чтении favorites.txt: " + e.getMessage());
        }
    }

    // Сохранить закладки в файл
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String url : favorites) {
                writer.write(url);
                writer.newLine();
            }
        } catch (Exception e) {
            System.out.println("Ошибка при записи favorites.txt: " + e.getMessage());
        }
    }
}
