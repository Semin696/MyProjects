package com.nightfallrealm.ui;

import com.nightfallrealm.Constants;
import com.nightfallrealm.core.MinecraftProcess;
import com.nightfallrealm.core.ModManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;

public class LauncherUI {

    private final Consumer<String> onPlay;
    private final Runnable onInstallMod;
    private final Runnable onInstallRP;
    private final Runnable onRemoveMod;
    private final Runnable onRemoveRP;
    private MinecraftProcess minecraftProcess;
    private Label fabricLabel;

    private Button playButton;
    private Label statusLabel;
    private Label serverStatusLabel;
    private Label modCountLabel;
    private Label rpCountLabel;
    private StarFieldCanvas starField;
    private StackPane contentPanel;
    private StackPane modsPanel;
    private StackPane rpPanel;
    private StackPane settingsPanel;
    private ListView<String> modsListView;
    private ListView<String> rpListView;
    private TextField usernameField;
    private Slider ramSlider;
    private Label ramValueLabel;
    private ToggleButton modsTab;
    private ToggleButton rpTab;
    private ToggleButton settingsTab;
    private ToggleGroup tabGroup;
    private Timeline pulseAnimation;

    private String currentUsername = "player";

    public LauncherUI(Consumer<String> onPlay,
                      Runnable onInstallMod,
                      Runnable onInstallRP,
                      Runnable onRemoveMod,
                      Runnable onRemoveRP) {
        this.onPlay = onPlay;
        this.onInstallMod = onInstallMod;
        this.onInstallRP = onInstallRP;
        this.onRemoveMod = onRemoveMod;
        this.onRemoveRP = onRemoveRP;
        loadUsername();
    }

    private void loadUsername() {
        File f = new File(Constants.LAUNCHER_DIR, "player.txt");
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String name = r.readLine();
                if (name != null && !name.trim().isEmpty()) currentUsername = name.trim();
            } catch (IOException ignored) {}
        }
    }

    public void saveUsername() {
        try {
            Constants.LAUNCHER_DIR.mkdirs();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(Constants.LAUNCHER_DIR, "player.txt")))) {
                w.write(currentUsername);
            }
        } catch (IOException ignored) {}
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();

        starField = new StarFieldCanvas(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        StackPane starPane = new StackPane(starField);
        starPane.setPickOnBounds(true);

        VBox overlay = new VBox();
        overlay.setPickOnBounds(false);

        HBox topBar = createTopBar();
        VBox centerContent = createCenterContent();
        HBox bottomBar = createBottomBar();

        VBox.setVgrow(centerContent, Priority.ALWAYS);
        overlay.getChildren().addAll(topBar, centerContent, bottomBar);

        StackPane mainStack = new StackPane(starPane, overlay);
        root.setCenter(mainStack);

        Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        scene.setFill(Color.web("#070720"));

        java.net.URL css = getClass().getResource("/css/theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        return scene;
    }

    private HBox createTopBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(15, 25, 10, 25));
        bar.setPickOnBounds(false);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);

        Text title = new Text("✦ Nightfall Realm SMP ✦");
        title.setFill(Color.web("#E0E0FF"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setEffect(new DropShadow(15, Color.web("#7B68EE", 0.5)));

        titleBox.getChildren().add(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox statusBox = new VBox(2);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        serverStatusLabel = new Label("●");
        serverStatusLabel.setTextFill(Color.web("#50FF50"));
        serverStatusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        serverStatusLabel.setEffect(new DropShadow(8, Color.web("#50FF50", 0.4)));

        Label serverLabel = new Label("Сервер онлайн");
        serverLabel.setTextFill(Color.web("#AAAACC"));
        serverLabel.setFont(Font.font("Segoe UI", 12));

        statusBox.getChildren().addAll(serverStatusLabel, serverLabel);

        bar.getChildren().addAll(titleBox, spacer, statusBox);
        return bar;
    }

    public void setMinecraftProcess(MinecraftProcess proc) {
        this.minecraftProcess = proc;
        updateFabricBadge();
    }

    private void updateFabricBadge() {
        if (minecraftProcess != null && minecraftProcess.isFabricInstalled()) {
            fabricLabel.setText("Fabric " + minecraftProcess.getFabricLoaderVersion());
            fabricLabel.setStyle("-fx-text-fill: #88DDFF; -fx-font-weight: bold;");
            fabricLabel.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#88DDFF", 0.3)));
        }
    }

    private VBox createCenterContent() {
        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.setSpacing(12);
        center.setPickOnBounds(false);
        center.setPadding(new Insets(20, 50, 10, 50));

        HBox versionRow = new HBox(8);
        versionRow.setAlignment(Pos.CENTER);

        Text versionLabel = new Text("Версия " + Constants.MINECRAFT_VERSION);
        versionLabel.setFill(Color.web("#7777AA"));
        versionLabel.setFont(Font.font("Segoe UI", 13));

        fabricLabel = new Label();
        fabricLabel.setTextFill(Color.web("#88DDFF"));
        fabricLabel.setFont(javafx.scene.text.Font.font("Segoe UI", 12));

        versionRow.getChildren().addAll(versionLabel, fabricLabel);

        playButton = new Button("▶  ИГРАТЬ");
        playButton.getStyleClass().add("play-button");
        playButton.setMinSize(220, 65);
        playButton.setMaxSize(220, 65);

        setupPlayButtonAnimation();

        playButton.setOnAction(e -> {
            if (usernameField != null) {
                currentUsername = usernameField.getText().trim();
                if (currentUsername.isEmpty()) currentUsername = "player";
            }
            saveUsername();
            onPlay.accept(currentUsername);
        });

        HBox tabBar = createTabBar();
        contentPanel = new StackPane();
        contentPanel.setMaxWidth(600);
        contentPanel.setMaxHeight(220);
        contentPanel.setPickOnBounds(true);

        modsPanel = createModsPanel();
        rpPanel = createRPanel();
        settingsPanel = createSettingsPanel();

        contentPanel.getChildren().addAll(modsPanel, rpPanel, settingsPanel);
        modsPanel.setVisible(true);
        rpPanel.setVisible(false);
        settingsPanel.setVisible(false);

        center.getChildren().addAll(versionRow, playButton, tabBar, contentPanel);
        return center;
    }

    private void setupPlayButtonAnimation() {
        Glow glow = new Glow();
        glow.setLevel(0.4);
        playButton.setEffect(glow);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#7B68EE", 0.5));
        shadow.setRadius(20);
        playButton.setEffect(shadow);

        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.radiusProperty(), 18),
                        new KeyValue(shadow.colorProperty(), Color.web("#7B68EE", 0.4))),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(shadow.radiusProperty(), 30),
                        new KeyValue(shadow.colorProperty(), Color.web("#9B88FF", 0.7))),
                new KeyFrame(Duration.seconds(3),
                        new KeyValue(shadow.radiusProperty(), 18),
                        new KeyValue(shadow.colorProperty(), Color.web("#7B68EE", 0.4)))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.setAutoReverse(true);
        pulseAnimation.play();

        playButton.setOnMouseEntered(e -> {
            playButton.setScaleX(1.05);
            playButton.setScaleY(1.05);
            shadow.setColor(Color.web("#9B88FF", 0.8));
            shadow.setRadius(35);
        });
        playButton.setOnMouseExited(e -> {
            playButton.setScaleX(1.0);
            playButton.setScaleY(1.0);
        });
        playButton.setOnMousePressed(e -> {
            playButton.setScaleX(0.95);
            playButton.setScaleY(0.95);
        });
        playButton.setOnMouseReleased(e -> {
            playButton.setScaleX(1.05);
            playButton.setScaleY(1.05);
        });
    }

    private HBox createTabBar() {
        tabGroup = new ToggleGroup();

        modsTab = new ToggleButton("📦 Моды");
        modsTab.setToggleGroup(tabGroup);
        modsTab.setSelected(true);
        modsTab.getStyleClass().add("tab-button");
        modsTab.setOnAction(e -> showPanel(modsPanel));

        rpTab = new ToggleButton("🎨 Ресурс-паки");
        rpTab.setToggleGroup(tabGroup);
        rpTab.getStyleClass().add("tab-button");
        rpTab.setOnAction(e -> showPanel(rpPanel));

        settingsTab = new ToggleButton("⚙ Настройки");
        settingsTab.setToggleGroup(tabGroup);
        settingsTab.getStyleClass().add("tab-button");
        settingsTab.setOnAction(e -> showPanel(settingsPanel));

        HBox tabBar = new HBox(8, modsTab, rpTab, settingsTab);
        tabBar.setAlignment(Pos.CENTER);
        tabBar.setPadding(new Insets(5, 0, 5, 0));
        tabBar.setPickOnBounds(false);
        return tabBar;
    }

    private void showPanel(StackPane panel) {
        modsPanel.setVisible(panel == modsPanel);
        rpPanel.setVisible(panel == rpPanel);
        settingsPanel.setVisible(panel == settingsPanel);
        refreshModsList();
        refreshRPList();
    }

    private StackPane createModsPanel() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("content-panel");

        VBox layout = new VBox(8);
        layout.setPadding(new Insets(10, 15, 10, 15));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Text modsTitle = new Text("Установленные моды");
        modsTitle.setFill(Color.web("#CCCCFF"));
        modsTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label();
        modCountLabel = countLabel;
        countLabel.setTextFill(Color.web("#8888BB"));
        countLabel.setFont(Font.font("Segoe UI", 12));

        Button addModBtn = new Button("+ Добавить мод");
        addModBtn.getStyleClass().add("small-button");
        addModBtn.setOnAction(e -> onInstallMod.run());

        header.getChildren().addAll(modsTitle, spacer, countLabel, addModBtn);

        modsListView = new ListView<>();
        modsListView.getStyleClass().add("mod-list");
        modsListView.setPrefHeight(120);
        modsListView.setPlaceholder(new Label("Нет установленных модов"));
        modsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ContextMenu modContextMenu = new ContextMenu();
        MenuItem removeModItem = new MenuItem("Удалить мод");
        removeModItem.setOnAction(e -> {
            String selected = modsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ModManager.removeMod(selected);
                refreshModsList();
                onRemoveMod.run();
            }
        });
        modContextMenu.getItems().add(removeModItem);
        modsListView.setContextMenu(modContextMenu);

        layout.getChildren().addAll(header, modsListView);
        pane.getChildren().add(layout);
        return pane;
    }

    private StackPane createRPanel() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("content-panel");

        VBox layout = new VBox(8);
        layout.setPadding(new Insets(10, 15, 10, 15));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Text rpTitle = new Text("Ресурс-паки");
        rpTitle.setFill(Color.web("#CCCCFF"));
        rpTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rpCountLabel = new Label();
        rpCountLabel.setTextFill(Color.web("#8888BB"));
        rpCountLabel.setFont(Font.font("Segoe UI", 12));

        Button addRPBtn = new Button("+ Добавить РП");
        addRPBtn.getStyleClass().add("small-button");
        addRPBtn.setOnAction(e -> onInstallRP.run());

        header.getChildren().addAll(rpTitle, spacer, rpCountLabel, addRPBtn);

        rpListView = new ListView<>();
        rpListView.getStyleClass().add("mod-list");
        rpListView.setPrefHeight(120);
        rpListView.setPlaceholder(new Label("Нет ресурс-паков"));
        rpListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ContextMenu rpContextMenu = new ContextMenu();
        MenuItem removeRPItem = new MenuItem("Удалить ресурс-пак");
        removeRPItem.setOnAction(e -> {
            String selected = rpListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ModManager.removeResourcePack(selected);
                refreshRPList();
                onRemoveRP.run();
            }
        });
        rpContextMenu.getItems().add(removeRPItem);
        rpListView.setContextMenu(rpContextMenu);

        layout.getChildren().addAll(header, rpListView);
        pane.getChildren().add(layout);
        return pane;
    }

    private StackPane createSettingsPanel() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("content-panel");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15, 20, 15, 20));

        Text settingsTitle = new Text("Настройки лаунчера");
        settingsTitle.setFill(Color.web("#CCCCFF"));
        settingsTitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));

        HBox usernameRow = new HBox(10);
        usernameRow.setAlignment(Pos.CENTER_LEFT);
        Label usernameLabel = new Label("Имя игрока:");
        usernameLabel.setTextFill(Color.web("#AAAACC"));
        usernameLabel.setFont(Font.font("Segoe UI", 13));
        usernameField = new TextField(currentUsername);
        usernameField.getStyleClass().add("settings-field");
        usernameField.setMaxWidth(200);
        usernameField.setPromptText("Введите ник");

        usernameRow.getChildren().addAll(usernameLabel, usernameField);

        HBox ramRow = new HBox(10);
        ramRow.setAlignment(Pos.CENTER_LEFT);
        Label ramLabel = new Label("Выделено RAM:");
        ramLabel.setTextFill(Color.web("#AAAACC"));
        ramLabel.setFont(Font.font("Segoe UI", 13));
        ramSlider = new Slider(1, 8, 3);
        ramSlider.setShowTickLabels(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setMajorTickUnit(1);
        ramSlider.setMinorTickCount(0);
        ramSlider.setSnapToTicks(true);
        ramSlider.setMaxWidth(300);
        ramSlider.getStyleClass().add("ram-slider");

        ramValueLabel = new Label("3 GB");
        ramValueLabel.setTextFill(Color.web("#CCCCFF"));
        ramValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        ramSlider.valueProperty().addListener((obs, old, val) ->
                ramValueLabel.setText(String.format("%.0f GB", val)));

        ramRow.getChildren().addAll(ramLabel, ramSlider, ramValueLabel);

        HBox gameDirRow = new HBox(10);
        gameDirRow.setAlignment(Pos.CENTER_LEFT);
        Label dirLabel = new Label("Папка игры:");
        dirLabel.setTextFill(Color.web("#AAAACC"));
        dirLabel.setFont(Font.font("Segoe UI", 13));
        Label dirValue = new Label(Constants.GAME_DIR.getAbsolutePath());
        dirValue.setTextFill(Color.web("#7777AA"));
        dirValue.setFont(Font.font("Segoe UI", 11));

        gameDirRow.getChildren().addAll(dirLabel, dirValue);

        layout.getChildren().addAll(settingsTitle, usernameRow, ramRow, gameDirRow);
        pane.getChildren().add(layout);
        return pane;
    }

    private HBox createBottomBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(8, 25, 12, 25));
        bar.setPickOnBounds(false);
        bar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("★ Готов к запуску");
        statusLabel.setTextFill(Color.web("#8888BB"));
        statusLabel.setFont(Font.font("Segoe UI", 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label versionLabel = new Label("v1.0.0");
        versionLabel.setTextFill(Color.web("#555577"));
        versionLabel.setFont(Font.font("Segoe UI", 11));

        bar.getChildren().addAll(statusLabel, spacer, versionLabel);
        return bar;
    }

    public void setStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            if (status.contains("Ошибка") || status.contains("ошибка")) {
                statusLabel.setTextFill(Color.web("#FF6666"));
            } else if (status.contains("Запуск") || status.contains("Готов")) {
                statusLabel.setTextFill(Color.web("#88FF88"));
            } else {
                statusLabel.setTextFill(Color.web("#8888BB"));
            }
        });
    }

    public void setPlayButtonEnabled(boolean enabled) {
        Platform.runLater(() -> {
            playButton.setDisable(!enabled);
            if (enabled) {
                playButton.setText("▶  ИГРАТЬ");
                if (pulseAnimation != null) pulseAnimation.play();
            } else {
                playButton.setText("⏳ Загрузка...");
                if (pulseAnimation != null) pulseAnimation.pause();
            }
        });
    }

    public void refreshModsList() {
        Platform.runLater(() -> {
            List<File> mods = ModManager.getMods();
            modsListView.getItems().clear();
            for (File f : mods) {
                modsListView.getItems().add(f.getName());
            }
            modCountLabel.setText(mods.size() + " модов");
            if (modsTab != null) {
                modsTab.setText("📦 Моды (" + mods.size() + ")");
            }
        });
    }

    public void refreshRPList() {
        Platform.runLater(() -> {
            List<File> rps = ModManager.getResourcePacks();
            rpListView.getItems().clear();
            for (File f : rps) {
                rpListView.getItems().add(f.getName());
            }
            rpCountLabel.setText(rps.size() + " РП");
            if (rpTab != null) {
                rpTab.setText("🎨 Ресурс-паки (" + rps.size() + ")");
            }
        });
    }

    public String getUsername() {
        return currentUsername;
    }

    public void stop() {
        if (pulseAnimation != null) pulseAnimation.stop();
        if (starField != null) starField.stop();
    }
}
