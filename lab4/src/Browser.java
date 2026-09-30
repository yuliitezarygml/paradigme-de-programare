import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Главное окно веб-браузера (Лабораторная работа 4)
// Расположение элементов как в Google Chrome:
// 1. В самом верху - строка вкладок (TabBar) с кнопкой "+"
// 2. Ниже - панель навигации (NavigationBar): кнопки Назад/Вперед/Обновить, Omnibox, История
// 3. Под ней - панель закладок (BookmarksBar)
// 4. По центру - страница сайта (JavaFX WebView WebKit)
// 5. Внизу - строка состояния (Status bar)
public class Browser extends JFrame {

    private final List<BrowserTab> tabs = new ArrayList<>();
    private int currentTabIndex = 0;

    private final FavoritesManager favoritesManager = new FavoritesManager();
    private final HistoryManager historyManager = new HistoryManager();

    private TabBar tabBar;
    private NavigationBar navBar;
    private BookmarksBar bookmarksBar;
    private JPanel contentPanel;
    private JLabel statusLabel;

    public Browser() {
        setTitle("Internet Browser - Laborator 4");
        setSize(1040, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        // Открываем первую вкладку с Google
        addNewTab("https://www.google.com");
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());

        // 1. Верхняя панель вкладок (как в Chrome в самом верху окна)
        tabBar = new TabBar(new TabBar.TabBarListener() {
            @Override public void onSelectTab(int index) { selectTab(index); }
            @Override public void onCloseTab(int index) { closeTab(index); }
            @Override public void onNewTab() { addNewTab("about:blank"); }
        });

        // 2. Панель навигации
        navBar = new NavigationBar(new NavigationBar.NavListener() {
            @Override public void onBack() { BrowserTab t = getActiveTab(); if (t != null) t.goBack(); }
            @Override public void onForward() { BrowserTab t = getActiveTab(); if (t != null) t.goForward(); }
            @Override public void onRefresh() { BrowserTab t = getActiveTab(); if (t != null) t.refresh(); }
            @Override public void onStop() { BrowserTab t = getActiveTab(); if (t != null) t.stopLoading(); }
            @Override public void onHome() { BrowserTab t = getActiveTab(); if (t != null) t.loadUrl("about:blank"); }
            @Override public void onNavigate(String url) { BrowserTab t = getActiveTab(); if (t != null) t.loadUrl(url); }
            @Override public void onAddFavorite() { addCurrentToFavorites(); }
            @Override public void onShowFavorites() {
                DialogHelper.showFavoritesDialog(Browser.this, favoritesManager,
                    url -> { BrowserTab t = getActiveTab(); if (t != null) t.loadUrl(url); },
                    () -> bookmarksBar.setBookmarks(favoritesManager.getFavorites())
                );
            }
            @Override public void onShowHistory() {
                DialogHelper.showHistoryDialog(Browser.this, historyManager,
                    url -> { BrowserTab t = getActiveTab(); if (t != null) t.loadUrl(url); }
                );
            }
        });

        // 3. Панель закладок (Bookmarks bar)
        bookmarksBar = new BookmarksBar(url -> {
            BrowserTab t = getActiveTab();
            if (t != null) t.loadUrl(url);
        });
        bookmarksBar.setBookmarks(favoritesManager.getFavorites());

        // Объединяем верхнюю часть: Вкладки -> Навигация -> Закладки
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(tabBar);
        topContainer.add(navBar);
        topContainer.add(bookmarksBar);

        // 4. Контейнер для отображения содержимого текущей вкладки
        contentPanel = new JPanel(new BorderLayout());

        // 5. Строка состояния внизу окна
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(3, 10, 3, 10));
        statusBar.setBackground(new Color(245, 245, 245));
        statusLabel = new JLabel("Gata");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);

        root.add(topContainer, BorderLayout.NORTH);
        root.add(contentPanel, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        add(root);
    }

    // Добавить новую вкладку
    public void addNewTab(String url) {
        BrowserTab tab = new BrowserTab(new BrowserTab.TabListener() {
            @Override
            public void onLoadingChanged(BrowserTab t, boolean loading) {
                if (t == getActiveTab()) {
                    navBar.setLoading(loading);
                    navBar.setBackEnabled(t.canGoBack());
                    navBar.setForwardEnabled(t.canGoForward());
                }
            }

            @Override
            public void onTitleChanged(BrowserTab t, String title) {
                tabBar.updateTabs(tabs, currentTabIndex);
                if (t == getActiveTab()) {
                    historyManager.addEntry(t.getCurrentUrl(), title);
                    setTitle(title + " - Internet Browser");
                }
            }

            @Override
            public void onUrlChanged(BrowserTab t, String newUrl) {
                if (t == getActiveTab()) {
                    navBar.setUrl(newUrl);
                    navBar.setBackEnabled(t.canGoBack());
                    navBar.setForwardEnabled(t.canGoForward());
                }
            }

            @Override
            public void onStatusChanged(BrowserTab t, String status) {
                if (t == getActiveTab()) {
                    statusLabel.setText(status);
                }
            }
        });

        tabs.add(tab);
        selectTab(tabs.size() - 1);

        if (url != null && !url.isEmpty()) {
            tab.loadUrl(url);
        }
    }

    // Выбрать вкладку по индексу
    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        currentTabIndex = index;
        BrowserTab tab = tabs.get(index);

        contentPanel.removeAll();
        contentPanel.add(tab.getContainerPanel(), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        tabBar.updateTabs(tabs, currentTabIndex);

        navBar.setUrl(tab.getCurrentUrl());
        navBar.setBackEnabled(tab.canGoBack());
        navBar.setForwardEnabled(tab.canGoForward());
        navBar.setLoading(tab.isLoading());
        setTitle(tab.getTitle() + " - Internet Browser");
    }

    // Закрыть вкладку
    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        BrowserTab tab = tabs.remove(index);
        tab.stopLoading();

        if (tabs.isEmpty()) {
            addNewTab("about:blank");
        } else {
            selectTab(Math.max(0, index - 1));
        }
    }

    public BrowserTab getActiveTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    // Добавить текущую страницу в закладки
    private void addCurrentToFavorites() {
        BrowserTab tab = getActiveTab();
        if (tab == null) return;
        String url = tab.getCurrentUrl();

        if ("about:blank".equalsIgnoreCase(url) || url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nu puteți adăuga pagina goală la favorite!", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (favoritesManager.addFavorite(url)) {
            bookmarksBar.setBookmarks(favoritesManager.getFavorites());
            JOptionPane.showMessageDialog(this, "Adresa a fost salvată în favorites.txt:\n" + url, "Adăugat la Favorite ⭐", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Această adresă este deja în favorite!", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
