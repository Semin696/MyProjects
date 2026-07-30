package com.nightfallrealm;

import com.nightfallrealm.core.MinecraftProcess;
import com.nightfallrealm.core.ModManager;
import com.nightfallrealm.ui.InstallerUI;
import com.nightfallrealm.ui.LauncherUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class Launcher extends Application {

    private LauncherUI launcherUI;
    private InstallerUI installerUI;
    private MinecraftProcess minecraftProcess;
    private Stage primaryStage;
    private Process gameProcess;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        Constants.loadConfig();
        stage.setTitle("Nightfall Realm SMP");
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/icon.png"));
            if (!icon.isError()) stage.getIcons().add(icon);
        } catch (Exception ignored) {}
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            if (launcherUI != null) launcherUI.stop();
            if (installerUI != null) installerUI.stop();
            if (gameProcess != null && gameProcess.isAlive()) {
                gameProcess.destroy();
            }
            Platform.exit();
        });

        if (isFirstRun()) {
            showInstaller();
        } else {
            showLauncher();
        }
    }

    private boolean isFirstRun() {
        File marker = new File(Constants.LAUNCHER_DIR, ".installed");
        return !marker.exists();
    }

    private void markInstalled() {
        try {
            Constants.LAUNCHER_DIR.mkdirs();
            new File(Constants.LAUNCHER_DIR, ".installed").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showInstaller() {
        installerUI = new InstallerUI(primaryStage, () -> {
            markInstalled();
            installerUI.stop();
            showLauncher();
        });
        primaryStage.setScene(installerUI.createScene());
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void showLauncher() {
        minecraftProcess = new MinecraftProcess();

        launcherUI = new LauncherUI(
                this::onPlay,
                this::onInstallMod,
                this::onInstallRP,
                this::onRemoveMod,
                this::onRemoveRP
        );

        primaryStage.setScene(launcherUI.createScene());
        primaryStage.centerOnScreen();
        primaryStage.show();

        launcherUI.setMinecraftProcess(minecraftProcess);
        launcherUI.refreshModsList();
        launcherUI.refreshRPList();
    }

    private void renameMinecraftWindow() {
        try {
            String[] script = {
                "$c = @'",
                "using System;",
                "using System.Runtime.InteropServices;",
                "public class W {",
                "  [DllImport(\"user32.dll\")]",
                "  public static extern bool SetWindowText(IntPtr h, string t);",
                "}",
                "'@",
                "Add-Type $c",
                "while ($true) {",
                "  Start-Sleep -Seconds 1",
                "  $p = Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' }",
                "  if ($p) {",
                "    [W]::SetWindowText($p.MainWindowHandle, 'Nightfall Realm SMP')",
                "    break",
                "  }",
                "}"
            };
            File scriptFile = new File(Constants.LAUNCHER_DIR, "rename.ps1");
            Files.write(scriptFile.toPath(), String.join("\n", script).getBytes());
            new ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File",
                    scriptFile.getAbsolutePath()).start();
        } catch (Exception ignored) {}
    }

    private void onPlay(String username) {
        launcherUI.setPlayButtonEnabled(false);
        launcherUI.setStatus("Подготовка игры...");

        CompletableFuture.runAsync(() -> {
            try {
                File modsGameDir = new File(Constants.GAME_DIR, "mods");
                modsGameDir.mkdirs();
                for (File mod : ModManager.getMods()) {
                    File target = new File(modsGameDir, mod.getName());
                    if (!target.exists()) {
                        java.nio.file.Files.copy(mod.toPath(), target.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                File rpGameDir = new File(Constants.GAME_DIR, "resourcepacks");
                rpGameDir.mkdirs();
                for (File rp : ModManager.getResourcePacks()) {
                    File target = new File(rpGameDir, rp.getName());
                    if (!target.exists()) {
                        java.nio.file.Files.copy(rp.toPath(), target.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    launcherUI.setStatus("Ошибка копирования модов: " + e.getMessage());
                    launcherUI.setPlayButtonEnabled(true);
                });
                return;
            }

            try {
                minecraftProcess.prepareGame(
                        status -> launcherUI.setStatus(status),
                        progress -> {}
                ).get();

                gameProcess = minecraftProcess.launch(username, status -> {
                    Platform.runLater(() -> launcherUI.setStatus(status));
                });

                launcherUI.setStatus("Minecraft запущен!");
                renameMinecraftWindow();

                new Thread(() -> {
                    try {
                        int exitCode = gameProcess.waitFor();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }).start();

                Platform.runLater(() -> {
                    primaryStage.hide();
                    launcherUI.stop();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    launcherUI.setStatus("Ошибка: " + e.getMessage());
                    launcherUI.setPlayButtonEnabled(true);

                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Не удалось запустить Minecraft.\n" + e.getMessage(),
                            ButtonType.OK);
                    alert.show();
                });
            }
        });
    }

    private void onInstallMod() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите мод (.jar)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Моды Minecraft", "*.jar"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            if (ModManager.addMod(file)) {
                launcherUI.refreshModsList();
                launcherUI.setStatus("Мод добавлен: " + file.getName());
            } else {
                launcherUI.setStatus("Ошибка при добавлении мода");
            }
        }
    }

    private void onInstallRP() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите ресурс-пак (.zip)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ресурс-паки", "*.zip"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            if (ModManager.addResourcePack(file)) {
                launcherUI.refreshRPList();
                launcherUI.setStatus("Ресурс-пак добавлен: " + file.getName());
            } else {
                launcherUI.setStatus("Ошибка при добавлении ресурс-пака");
            }
        }
    }

    private void onRemoveMod() {
        launcherUI.refreshModsList();
        launcherUI.setStatus("Мод удалён");
    }

    private void onRemoveRP() {
        launcherUI.refreshRPList();
        launcherUI.setStatus("Ресурс-пак удалён");
    }
}
