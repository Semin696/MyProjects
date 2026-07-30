package com.nightfallrealm.ui;

import com.nightfallrealm.Constants;
import com.nightfallrealm.Launcher;
import com.nightfallrealm.core.MinecraftProcess;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class InstallerUI {

    private final Stage stage;
    private final Runnable onComplete;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Text titleText;
    private Button launchButton;
    private StarFieldCanvas starField;
    private StackPane mainContent;
    private VBox welcomeContent;
    private VBox progressContent;
    private VBox completeContent;
    private TextField gameDirField;
    private File chosenGameDir;

    public InstallerUI(Stage stage, Runnable onComplete) {
        this.stage = stage;
        this.onComplete = onComplete;
        this.chosenGameDir = Constants.GAME_DIR;
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();

        starField = new StarFieldCanvas(Constants.INSTALLER_WIDTH, Constants.INSTALLER_HEIGHT);
        StackPane starPane = new StackPane(starField);
        starPane.setPickOnBounds(true);

        mainContent = new StackPane();
        mainContent.setPickOnBounds(false);
        mainContent.setPadding(new Insets(30, 40, 30, 40));

        welcomeContent = createWelcomeContent();
        progressContent = createProgressContent();
        completeContent = createCompleteContent();

        mainContent.getChildren().addAll(welcomeContent, progressContent, completeContent);
        welcomeContent.setVisible(true);
        progressContent.setVisible(false);
        completeContent.setVisible(false);

        StackPane stack = new StackPane(starPane, mainContent);
        root.setCenter(stack);

        Scene scene = new Scene(root, Constants.INSTALLER_WIDTH, Constants.INSTALLER_HEIGHT);
        scene.setFill(Color.web("#070720"));

        java.net.URL css = getClass().getResource("/css/theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        return scene;
    }

    private VBox createWelcomeContent() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(550);

        Text starIcon = new Text("✦");
        starIcon.setFill(Color.web("#C8C8FF"));
        starIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60));
        starIcon.setEffect(new DropShadow(25, Color.web("#7B68EE", 0.6)));

        FadeTransition ft = new FadeTransition(Duration.seconds(2), starIcon);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        titleText = new Text("Nightfall Realm SMP");
        titleText.setFill(Color.web("#E0E0FF"));
        titleText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
        titleText.setTextAlignment(TextAlignment.CENTER);
        titleText.setEffect(new DropShadow(20, Color.web("#7B68EE", 0.5)));

        Text subtitle = new Text("Установка лаунчера");
        subtitle.setFill(Color.web("#8888BB"));
        subtitle.setFont(Font.font("Segoe UI", 18));

        HBox dirRow = new HBox(8);
        dirRow.setAlignment(Pos.CENTER);
        dirRow.setPadding(new Insets(5, 0, 5, 0));

        Label dirLabel = new Label("Папка игры:");
        dirLabel.setTextFill(Color.web("#AAAACC"));
        dirLabel.setFont(Font.font("Segoe UI", 13));

        gameDirField = new TextField(chosenGameDir.getAbsolutePath());
        gameDirField.getStyleClass().add("settings-field");
        gameDirField.setPrefWidth(350);

        Button browseBtn = new Button("Обзор...");
        browseBtn.getStyleClass().add("small-button");
        browseBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Выберите папку для игры");
            File selected = dc.showDialog(stage);
            if (selected != null) {
                chosenGameDir = selected;
                gameDirField.setText(selected.getAbsolutePath());
            }
        });

        dirRow.getChildren().addAll(dirLabel, gameDirField, browseBtn);

        Region spacer = new Region();
        spacer.setPrefHeight(20);

        Button startBtn = new Button("▶  НАЧАТЬ УСТАНОВКУ");
        startBtn.getStyleClass().add("install-button");
        startBtn.setMinSize(280, 55);
        startBtn.setOnAction(e -> startInstallation());

        box.getChildren().addAll(starIcon, titleText, subtitle, dirRow, spacer, startBtn);
        return box;
    }

    private VBox createProgressContent() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);

        Text installingText = new Text("Установка...");
        installingText.setFill(Color.web("#CCCCFF"));
        installingText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));

        progressBar = new ProgressBar(0);
        progressBar.setMinWidth(450);
        progressBar.setMinHeight(25);
        progressBar.getStyleClass().add("install-progress");

        statusLabel = new Label("Подготовка...");
        statusLabel.setTextFill(Color.web("#8888BB"));
        statusLabel.setFont(Font.font("Segoe UI", 14));

        box.getChildren().addAll(installingText, progressBar, statusLabel);
        return box;
    }

    private VBox createCompleteContent() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);

        Text checkMark = new Text("✦");
        checkMark.setFill(Color.web("#88FF88"));
        checkMark.setFont(Font.font("Segoe UI", FontWeight.BOLD, 60));
        checkMark.setEffect(new DropShadow(25, Color.web("#88FF88", 0.5)));

        Text completeTitle = new Text("Установка завершена!");
        completeTitle.setFill(Color.web("#E0E0FF"));
        completeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));

        Text completeDesc = new Text("Лаунчер готов к работе. Кастомное меню\n"
                + "в стиле Nightfall Realm SMP установлено.");
        completeDesc.setFill(Color.web("#8888BB"));
        completeDesc.setFont(Font.font("Segoe UI", 14));
        completeDesc.setTextAlignment(TextAlignment.CENTER);

        HBox fabricRow = new HBox(6);
        fabricRow.setAlignment(Pos.CENTER);
        Label fabricIcon = new Label("◆");
        fabricIcon.setTextFill(Color.web("#88DDFF"));
        fabricIcon.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        fabricIcon.setEffect(new DropShadow(10, Color.web("#88DDFF", 0.4)));
        Label fabricStatus = new Label("Fabric Loader установлен");
        fabricStatus.setTextFill(Color.web("#88DDFF"));
        fabricStatus.setFont(Font.font("Segoe UI", 14));
        fabricRow.getChildren().addAll(fabricIcon, fabricStatus);

        Region spacer2 = new Region();
        spacer2.setPrefHeight(20);

        launchButton = new Button("✦  ЗАПУСТИТЬ ЛАУНЧЕР");
        launchButton.getStyleClass().add("install-button");
        launchButton.setMinSize(280, 55);
        launchButton.setOnAction(e -> onComplete.run());

        box.getChildren().addAll(checkMark, completeTitle, completeDesc, fabricRow, spacer2, launchButton);
        return box;
    }

    private void startInstallation() {
        chosenGameDir = new File(gameDirField.getText().trim());
        Constants.GAME_DIR = chosenGameDir;
        Constants.LAUNCHER_DIR = new File(chosenGameDir, "launcher");
        Constants.JAVA_DIR = new File(Constants.LAUNCHER_DIR, "java21");
        Constants.MODS_DIR = new File(chosenGameDir, "mods");
        Constants.RESOURCE_PACKS_DIR = new File(chosenGameDir, "resourcepacks");
        Constants.saveConfig();

        welcomeContent.setVisible(false);
        progressContent.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), progressContent);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        new Thread(this::doInstallation).start();
    }

    private void doInstallation() {
        try {
            Platform.runLater(() -> { statusLabel.setText("Создание папок..."); progressBar.setProgress(0.05); });
            Constants.MODS_DIR.mkdirs();
            Constants.RESOURCE_PACKS_DIR.mkdirs();
            Constants.GAME_DIR.mkdirs();
            Constants.LAUNCHER_DIR.mkdirs();
            Thread.sleep(300);

            Platform.runLater(() -> { statusLabel.setText("Настройка директорий..."); progressBar.setProgress(0.15); });
            new File(Constants.GAME_DIR, "libraries").mkdirs();
            new File(Constants.GAME_DIR, "versions").mkdirs();
            new File(Constants.GAME_DIR, "assets").mkdirs();
            new File(Constants.GAME_DIR, "natives").mkdirs();
            new File(Constants.GAME_DIR, "mods").mkdirs();
            new File(Constants.GAME_DIR, "resourcepacks").mkdirs();
            Thread.sleep(300);

            Platform.runLater(() -> { statusLabel.setText("Проверка Java 21..."); progressBar.setProgress(0.2); });
            try {
                MinecraftProcess.downloadJava21(
                    s -> Platform.runLater(() -> statusLabel.setText(s))
                );
                Platform.runLater(() -> progressBar.setProgress(0.25));
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Java 21: " + e.getMessage()));
            }
            Thread.sleep(200);

            Platform.runLater(() -> { statusLabel.setText("Загрузка Fabric Loader..."); progressBar.setProgress(0.3); });
            try {
                MinecraftProcess mc = new MinecraftProcess();
                mc.prepareGame(
                    s -> Platform.runLater(() -> statusLabel.setText(s)),
                    p -> Platform.runLater(() -> progressBar.setProgress(0.3 + p * 0.5))
                ).get();
                if (mc.isFabricInstalled()) {
                    Platform.runLater(() -> statusLabel.setText("Fabric " + mc.getFabricLoaderVersion() + " установлен"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Fabric: " + e.getMessage()));
            }
            Thread.sleep(300);

            Platform.runLater(() -> { statusLabel.setText("Установка мода NightfallMenu..."); progressBar.setProgress(0.85); });
            try {
                File modJar = new File(Constants.MODS_DIR, "nightfallmenu-1.0.0.jar");
                if (!modJar.exists()) {
                    File launcherJar = new File(Launcher.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI());
                    File launcherDir = launcherJar.getParentFile();
                    File modDir = new File(launcherDir, "mod-nightfallmenu");
                    if (!modDir.exists()) {
                        modDir = new File(System.getProperty("user.dir"), "mod-nightfallmenu");
                    }
                    if (!modDir.exists()) {
                        modDir = new File(System.getProperty("user.dir"), "app/mod-nightfallmenu");
                    }
                    if (modDir.exists()) {
                        MinecraftProcess.downloadJava21(s -> Platform.runLater(() -> statusLabel.setText(s)));
                        String java21 = MinecraftProcess.findJava21();
                        ProcessBuilder pb = new ProcessBuilder(
                                "cmd", "/c", "gradlew.bat", "build", "-q");
                        pb.directory(modDir);
                        pb.environment().put("JAVA_HOME",
                                new File(java21).getParentFile().getParentFile().getAbsolutePath());
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        p.waitFor(120, TimeUnit.SECONDS);
                        File builtJar = new File(modDir, "build/libs/nightfallmenu-1.0.0.jar");
                        if (builtJar.exists()) {
                            java.nio.file.Files.copy(builtJar.toPath(), modJar.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            Platform.runLater(() -> statusLabel.setText("Мод NightfallMenu установлен"));
                        }
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Мод NightfallMenu: " + e.getMessage()));
            }
            Thread.sleep(300);

            Platform.runLater(() -> { statusLabel.setText("Создание ярлыка на рабочем столе..."); progressBar.setProgress(0.95); });
            try {
                createDesktopShortcut();
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Ярлык: " + e.getMessage()));
            }
            Thread.sleep(200);

            Platform.runLater(() -> { statusLabel.setText("Установка завершена!"); progressBar.setProgress(1.0); });
            Thread.sleep(500);

            Platform.runLater(this::showCompletion);
        } catch (InterruptedException e) {
            Platform.runLater(() -> statusLabel.setText("Установка прервана"));
        }
    }

    private void createDesktopShortcut() throws Exception {
        String desktop = System.getProperty("user.home") + "/Desktop";
        File shortcut = new File(desktop, "Nightfall Realm SMP.lnk");
        if (shortcut.exists()) return;

        File launcherJar = new File(Launcher.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        File appDir = launcherJar.getParentFile().getParentFile();
        String exePath = appDir.getAbsolutePath() + "/Nightfall Realm SMP.exe";
        File exeFile = new File(exePath);
        if (!exeFile.exists()) {
            exePath = appDir.getAbsolutePath() + "/Nightfall Realm SMP/Nightfall Realm SMP.exe";
            exeFile = new File(exePath);
        }
        if (!exeFile.exists()) return;

        String ps = String.format(
            "$ws = New-Object -ComObject WScript.Shell; " +
            "$s = $ws.CreateShortcut('%s'); " +
            "$s.TargetPath = '%s'; $s.Description = 'Nightfall Realm SMP Launcher'; " +
            "$s.WorkingDirectory = '%s'; $s.Save()",
            shortcut.getAbsolutePath().replace("'", "''"),
            exeFile.getAbsolutePath().replace("'", "''"),
            appDir.getAbsolutePath().replace("'", "''"));
        new ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-Command", ps)
                .redirectErrorStream(true)
                .start()
                .waitFor(10, TimeUnit.SECONDS);
    }

    private void showCompletion() {
        progressContent.setVisible(false);
        completeContent.setVisible(true);

        ScaleTransition st = new ScaleTransition(Duration.seconds(0.5), completeContent);
        st.setFromX(0.5);
        st.setFromY(0.5);
        st.setToX(1);
        st.setToY(1);

        FadeTransition ft = new FadeTransition(Duration.seconds(0.5), completeContent);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
    }

    public void stop() {
        if (starField != null) starField.stop();
    }
}
