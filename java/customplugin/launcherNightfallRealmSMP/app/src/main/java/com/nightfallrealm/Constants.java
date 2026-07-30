package com.nightfallrealm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class Constants {
    public static final String LAUNCHER_NAME = "Nightfall Realm SMP";
    public static final String SERVER_IP = "f1.rustix.me";
    public static final int SERVER_PORT = 25283;
    public static final String MINECRAFT_VERSION = "1.21.11";

    public static final File DEFAULT_GAME_DIR = new File(System.getenv("APPDATA"), "NightfallRealmSMP");
    public static File GAME_DIR = DEFAULT_GAME_DIR;
    public static File MODS_DIR = new File("C:/NightfallRealmSMP/mods");
    public static File RESOURCE_PACKS_DIR = new File("C:/NightfallRealmSMP/resourcepacks");
    public static File LAUNCHER_DIR = new File(GAME_DIR, "launcher");
    public static File JAVA_DIR = new File(LAUNCHER_DIR, "java21");

    public static final File CONFIG_FILE = new File(System.getenv("APPDATA")
            + "/NightfallRealmSMP/launcher/config.properties");

    public static final String VERSION_MANIFEST_URL =
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String LIBRARIES_BASE_URL = "https://libraries.minecraft.net";
    public static final String ASSETS_BASE_URL = "https://resources.download.minecraft.net";

    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 680;
    public static final int INSTALLER_WIDTH = 820;
    public static final int INSTALLER_HEIGHT = 620;

    public static final int MIN_RAM_MB = 1024;
    public static final int RECOMMENDED_RAM_MB = 3072;

    public static final String DOWNLOAD_USER_AGENT = "NightfallLauncher/1.0";

    public static final String FABRIC_META_URL =
        "https://meta.fabricmc.net/v2/versions/loader/%s";
    public static final String FABRIC_PROFILE_URL =
        "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";
    public static final String FABRIC_MAVEN_URL = "https://maven.fabricmc.net/";
    public static final String FABRIC_MAIN_CLASS =
        "net.fabricmc.loader.impl.launch.knot.KnotClient";

    public static final String JAVA_DOWNLOAD_URL =
        "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse";

    public static void loadConfig() {
        Properties props = new Properties();
        if (CONFIG_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);
                String gameDir = props.getProperty("game.dir");
                if (gameDir != null && !gameDir.isEmpty()) {
                    GAME_DIR = new File(gameDir);
                    LAUNCHER_DIR = new File(GAME_DIR, "launcher");
                    JAVA_DIR = new File(LAUNCHER_DIR, "java21");
                    MODS_DIR = new File(GAME_DIR, "mods");
                    RESOURCE_PACKS_DIR = new File(GAME_DIR, "resourcepacks");
                }
            } catch (IOException e) {
                // use defaults
            }
        }
    }

    public static void saveConfig() {
        Properties props = new Properties();
        props.setProperty("game.dir", GAME_DIR.getAbsolutePath());
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Nightfall Realm SMP Launcher Config");
        } catch (IOException e) {
            // ignore
        }
    }

    private Constants() {}
}
