package aethereal.util;

import aethereal.core.Interface;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ClientRestart {
    private ClientRestart() {
    }

    public static void restart() {
        try {
            File root = projectRoot();
            File runDir = new File(root, "run");
            if (!runDir.isDirectory()) {
                runDir.mkdirs();
            }
            Files.writeString(new File(runDir, "malice-autoplay").toPath(), "play", StandardCharsets.UTF_8);

            File exe = new File(root, "MaliceVisuals.exe");
            File ps1 = new File(root, "MaliceLauncher.ps1");
            String launch;
            if (exe.isFile()) {
                launch = "start \"Malice Visuals\" /D \"" + root.getAbsolutePath() + "\" \"" + exe.getAbsolutePath() + "\"";
            } else if (ps1.isFile()) {
                launch = "start \"Malice Visuals\" /D \"" + root.getAbsolutePath()
                        + "\" powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File \""
                        + ps1.getAbsolutePath() + "\"";
            } else {
                launch = null;
            }
            if (launch != null) {
                ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", launch);
                builder.directory(root);
                builder.start();
            }
        } catch (IOException ignored) {
        }
        if (Interface.mc != null) {
            Interface.mc.scheduleStop();
        }
    }

    private static File projectRoot() {
        File dir = new File(System.getProperty("user.dir", "."));
        String name = dir.getName();
        if (name != null && "run".equalsIgnoreCase(name) && dir.getParentFile() != null) {
            return dir.getParentFile();
        }
        File marker = new File(dir, "MaliceLauncher.ps1");
        if (!marker.isFile() && dir.getParentFile() != null && new File(dir.getParentFile(), "MaliceLauncher.ps1").isFile()) {
            return dir.getParentFile();
        }
        return dir;
    }
}
