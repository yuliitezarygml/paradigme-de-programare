import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Представляет одну вкладку браузера на основе JavaFX WebView
// Встроена в Swing через JFXPanel для полноценной поддержки современных сайтов (YouTube, JS, CSS3)
// Содержит центральную карточку визуализации HTTP-запросов и процесса загрузки
public class BrowserTab {

    public interface TabListener {
        void onLoadingChanged(BrowserTab tab, boolean loading);
        void onTitleChanged(BrowserTab tab, String title);
        void onUrlChanged(BrowserTab tab, String url);
        void onStatusChanged(BrowserTab tab, String status);
    }

    private final JFXPanel jfxPanel;
    private final JPanel containerPanel;
    private final TabListener listener;

    private WebView webView;
    private WebEngine webEngine;

    // Элементы визуализации запросов и загрузки по центру экрана
    private VBox loadingCard;
    private Label loadingTitleLabel;
    private Label loadingUrlLabel;
    private Label loadingStatusLabel;
    private Label loadingProtocolLabel;
    private ProgressBar loadingProgressBar;
    private Button retryBtn;

    private String currentUrl = "about:blank";
    private String title = "Pagină nouă";
    private boolean loading = false;
    private boolean canBack = false;
    private boolean canForward = false;

    public BrowserTab(TabListener listener) {
        this.listener = listener;
        this.jfxPanel = new JFXPanel();
        this.containerPanel = new JPanel(new BorderLayout());
        this.containerPanel.add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(this::initJavaFX);
    }

    private void initJavaFX() {
        webView = new WebView();
        webEngine = webView.getEngine();

        // Устанавливаем современный User-Agent браузера
        webEngine.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        // Создаем центральную карточку отображения сетевых запросов
        createLoadingCard();

        // Слушатель изменения заголовка страницы
        webEngine.titleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                this.title = newVal.trim();
                SwingUtilities.invokeLater(() -> {
                    if (listener != null) listener.onTitleChanged(this, this.title);
                });
            }
        });

        // Слушатель изменения текущего адреса URL
        webEngine.locationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                this.currentUrl = newVal.trim();
                SwingUtilities.invokeLater(() -> {
                    if (listener != null) listener.onUrlChanged(this, this.currentUrl);
                });
            }
        });

        // Слушатель прогресса загрузки (0% -> 100%)
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            double p = newVal.doubleValue();
            if (p < 0) p = 0;
            loadingProgressBar.setProgress(p);
            int percent = (int) (p * 100);

            if (percent < 20) {
                loadingStatusLabel.setText("📡 Trimitere cerere HTTP GET către server (" + percent + "%)...");
            } else if (percent < 50) {
                loadingStatusLabel.setText("📥 Descărcare conținut HTML și scripturi (" + percent + "%)...");
            } else if (percent < 85) {
                loadingStatusLabel.setText("🎨 Procesare CSS și construire arbore DOM (" + percent + "%)...");
            } else {
                loadingStatusLabel.setText("✅ Finalizare randare pagină (" + percent + "%)...");
            }
        });

        // Слушатель состояния загрузки (RUNNING, SUCCEEDED, FAILED)
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                if (!"about:blank".equalsIgnoreCase(currentUrl)) {
                    loadingCard.setVisible(true);
                    loadingTitleLabel.setText("⏳ Se încarcă pagina...");
                    loadingTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
                    loadingUrlLabel.setText(currentUrl);
                    loadingProgressBar.setVisible(true);
                    loadingProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    loadingStatusLabel.setText("📡 Conectare la server...");
                    retryBtn.setVisible(false);
                }
            } else if (newState == Worker.State.SUCCEEDED) {
                loadingCard.setVisible(false);
            } else if (newState == Worker.State.FAILED) {
                if (!"about:blank".equalsIgnoreCase(currentUrl)) {
                    loadingCard.setVisible(true);
                    loadingTitleLabel.setText("❌ Eroare de conectare");
                    loadingTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #d93025;");
                    loadingProgressBar.setVisible(false);
                    loadingStatusLabel.setText("Nu s-a putut încărca adresa solicitată.\nVerificați conexiunea la internet sau corectitudinea adresei.");
                    retryBtn.setVisible(true);
                }
            } else {
                loadingCard.setVisible(false);
            }

            SwingUtilities.invokeLater(() -> {
                if (newState == Worker.State.RUNNING) {
                    this.loading = true;
                    if (listener != null) {
                        listener.onLoadingChanged(this, true);
                        listener.onStatusChanged(this, "Încărcare... " + currentUrl);
                    }
                } else {
                    this.loading = false;
                    updateNavigationHistory();
                    if (listener != null) {
                        listener.onLoadingChanged(this, false);
                        listener.onStatusChanged(this, newState == Worker.State.SUCCEEDED ? "Gata" : "Eroare / Oprit");
                    }
                }
            });
        });

        // Размещаем WebView и карточку загрузки в StackPane (карточка всплывает по центру поверх страницы)
        StackPane rootPane = new StackPane();
        rootPane.getChildren().addAll(webView, loadingCard);
        StackPane.setAlignment(loadingCard, Pos.CENTER);

        Scene scene = new Scene(rootPane);
        jfxPanel.setScene(scene);

        // Показываем стартовую страницу
        loadWelcomePage();
    }

    // Создание красивой центральной карточки сетевого запроса
    private void createLoadingCard() {
        loadingCard = new VBox(10);
        loadingCard.setAlignment(Pos.CENTER);
        loadingCard.setMaxSize(440, 220);
        loadingCard.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.96);" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: #d1d5db;" +
            "-fx-border-radius: 14px;" +
            "-fx-border-width: 1px;" +
            "-fx-padding: 20px 26px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 18, 0, 0, 6);"
        );
        loadingCard.setVisible(false);

        loadingTitleLabel = new Label("⏳ Se încarcă pagina...");
        loadingTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");

        loadingUrlLabel = new Label("https://www.google.com");
        loadingUrlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368; -fx-wrap-text: true;");
        loadingUrlLabel.setMaxWidth(380);

        loadingProgressBar = new ProgressBar(0);
        loadingProgressBar.setPrefWidth(380);
        loadingProgressBar.setPrefHeight(9);
        loadingProgressBar.setStyle("-fx-accent: #1a73e8;");

        loadingStatusLabel = new Label("📡 Trimitere cerere GET...");
        loadingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-font-weight: bold; -fx-text-alignment: center;");
        loadingStatusLabel.setWrapText(true);
        loadingStatusLabel.setMaxWidth(380);

        loadingProtocolLabel = new Label("Metodă: HTTP GET | Protocol: TLS / HTTPS (Securizat 🔒)");
        loadingProtocolLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        retryBtn = new Button("🔄 Reîncearcă (Retry)");
        retryBtn.setStyle("-fx-font-size: 12px; -fx-background-color: #1a73e8; -fx-text-fill: white; -fx-padding: 6px 14px; -fx-background-radius: 6px;");
        retryBtn.setVisible(false);
        retryBtn.setOnAction(e -> refresh());

        loadingCard.getChildren().addAll(
            loadingTitleLabel,
            loadingUrlLabel,
            loadingProgressBar,
            loadingStatusLabel,
            loadingProtocolLabel,
            retryBtn
        );
    }

    // Обновление состояния кнопок Назад / Вперед
    private void updateNavigationHistory() {
        if (webEngine == null) return;
        WebHistory history = webEngine.getHistory();
        int idx = history.getCurrentIndex();
        this.canBack = idx > 0;
        this.canForward = idx < history.getEntries().size() - 1;
    }

    // Распознает, введен ли готовый URL или поисковый запрос
    public static String resolveUrlOrSearch(String input) {
        if (input == null || input.trim().isEmpty()) return "about:blank";
        String clean = input.trim();
        if ("about:blank".equalsIgnoreCase(clean)) return "about:blank";

        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("file://")) {
            return clean;
        }
        if (clean.contains(".") && !clean.contains(" ")) {
            return "https://" + clean;
        }

        // Если введен текст с пробелами или без точки - ищем через Google
        try {
            return "https://www.google.com/search?q=" + URLEncoder.encode(clean, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "https://www.google.com/search?q=" + clean;
        }
    }

    // Загрузить страницу по адресу
    public void loadUrl(String input) {
        String target = resolveUrlOrSearch(input);
        this.currentUrl = target;
        System.out.println("Загружаем страницу: " + target);

        Platform.runLater(() -> {
            if ("about:blank".equalsIgnoreCase(target)) {
                loadWelcomePage();
            } else {
                if (loadingCard != null) {
                    loadingCard.setVisible(true);
                    loadingTitleLabel.setText("⏳ Se încarcă pagina...");
                    loadingTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a73e8;");
                    loadingUrlLabel.setText(target);
                    loadingProgressBar.setVisible(true);
                    loadingProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    loadingStatusLabel.setText("📡 Conectare la " + target + "...");
                    retryBtn.setVisible(false);
                }
                if (webEngine != null) {
                    webEngine.load(target);
                }
            }
        });
    }

    public void goBack() {
        Platform.runLater(() -> {
            if (webEngine != null && webEngine.getHistory().getCurrentIndex() > 0) {
                webEngine.getHistory().go(-1);
            }
        });
    }

    public void goForward() {
        Platform.runLater(() -> {
            if (webEngine != null) {
                WebHistory h = webEngine.getHistory();
                if (h.getCurrentIndex() < h.getEntries().size() - 1) {
                    h.go(1);
                }
            }
        });
    }

    public void refresh() {
        Platform.runLater(() -> {
            if (webEngine != null) webEngine.reload();
        });
    }

    public void stopLoading() {
        Platform.runLater(() -> {
            if (webEngine != null) {
                webEngine.getLoadWorker().cancel();
                if (loadingCard != null) loadingCard.setVisible(false);
            }
        });
    }

    // Красивая стартовая страница
    private void loadWelcomePage() {
        this.title = "Pagină nouă";
        if (loadingCard != null) loadingCard.setVisible(false);

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<style>"
            + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f8f9fa; color: #202124; text-align: center; padding-top: 60px; }"
            + "h1 { font-size: 38px; color: #1a73e8; margin-bottom: 5px; }"
            + "p { color: #5f6368; font-size: 15px; margin-bottom: 30px; }"
            + ".grid { display: flex; justify-content: center; gap: 16px; flex-wrap: wrap; max-width: 600px; margin: 0 auto; }"
            + ".card { background: white; border: 1px solid #dadce0; border-radius: 12px; padding: 18px 24px; text-decoration: none; color: #202124; font-weight: 500; font-size: 15px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); transition: transform 0.15s; }"
            + ".card:hover { transform: translateY(-3px); box-shadow: 0 4px 10px rgba(0,0,0,0.12); color: #1a73e8; }"
            + "</style></head><body>"
            + "<h1>Internet Browser</h1>"
            + "<p>Laborator 4 - JavaFX WebView Engine (WebKit)</p>"
            + "<div class='grid'>"
            + "<a class='card' href='https://www.youtube.com'>&#9654; YouTube</a>"
            + "<a class='card' href='https://www.google.com'>&#128269; Google</a>"
            + "<a class='card' href='https://en.wikipedia.org'>&#128214; Wikipedia</a>"
            + "<a class='card' href='https://github.com'>&#128187; GitHub</a>"
            + "</div></body></html>";

        if (webEngine != null) {
            webEngine.loadContent(html, "text/html; charset=UTF-8");
        }
    }

    public JPanel getContainerPanel() { return containerPanel; }
    public String getCurrentUrl() { return currentUrl; }
    public String getTitle() { return title; }
    public boolean isLoading() { return loading; }
    public boolean canGoBack() { return canBack; }
    public boolean canGoForward() { return canForward; }
}
