package aethereal.discord;

import aethereal.lib.log4j.LogManager;
import aethereal.lib.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

final class DiscordPipeFix {
    private static final Logger LOGGER = LogManager.b(DiscordPipeFix.class);
    private static final AtomicBoolean attempted = new AtomicBoolean();

    private DiscordPipeFix() {
    }

    static boolean a() {
        return attempted.get();
    }

    static boolean b() {
        if (!attempted.compareAndSet(false, true)) {
            return false;
        }
        try {
            stopDiscordProcesses();
            Thread.sleep(1500L);
            if (canOpenPipe()) {
                LOGGER.a("Discord IPC pipe is now accessible");
                return true;
            }
            requestUnelevatedRestart();
            return true;
        } catch (Exception e) {
            LOGGER.b("Failed to recover Discord IPC: {}", e.getMessage());
            return false;
        }
    }

    private static void stopDiscordProcesses() {
        ProcessHandle.allProcesses().forEach(handle -> {
            String command = handle.info().command().orElse("").replace('\\', '/').toLowerCase(Locale.ROOT);
            if (command.endsWith("/discord.exe") || command.endsWith("/discordsystemhelper.exe")) {
                handle.destroyForcibly();
            }
        });
    }

    private static boolean canOpenPipe() {
        try {
            new WindowsConnection("\\\\.\\pipe\\discord-ipc-0").close();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void requestUnelevatedRestart() throws Exception {
        Path script = Files.createTempFile("malice-discord-fix", ".ps1");
        String body = String.join("\r\n",
                "Stop-Process -Name Discord,DiscordSystemHelper -Force -ErrorAction SilentlyContinue",
                "Start-Sleep -Seconds 2",
                "$update = Join-Path $env:LOCALAPPDATA 'Discord\\Update.exe'",
                "$bat = Join-Path $env:TEMP 'malice-start-discord.cmd'",
                "$lines = @(",
                "  '@echo off',",
                "  ('start \"\" \"' + $update + '\" --processStart Discord.exe')",
                ")",
                "Set-Content -Path $bat -Value $lines -Encoding ASCII",
                "Start-Process -FilePath \"$env:WINDIR\\explorer.exe\" -ArgumentList $bat"
        );
        Files.writeString(script, body, StandardCharsets.UTF_8);
        String file = script.toAbsolutePath().toString().replace("'", "''");
        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-Command",
                "Start-Process -FilePath powershell.exe -Verb RunAs -WindowStyle Hidden -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','" + file + "')"
        ).inheritIO().start();
        LOGGER.a("Asked Windows to restart Discord without administrator rights");
    }
}
