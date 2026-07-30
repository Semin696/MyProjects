package org.nig.smp.autoRestart;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class AutoRestart extends JavaPlugin implements Listener {

    private BossBar bossBar;
    private int taskId = -1;
    private boolean countingDown = false;
    private int countdownSeconds;

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("restart_server").setExecutor(this::onCommand);
        Bukkit.getPluginManager().registerEvents(this, this);
        scheduleDailyRestart();
        getLogger().info("AutoRestart enabled");
    }

    @Override
    public void onDisable() {
        cancelCountdown();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    private String color(String s) {
        return s.replace('&', '§');
    }

    private String prefix() {
        return color(getConfig().getString("prefix", ""));
    }

    private String msg(String key) {
        return color(getConfig().getString("messages." + key, "").replace("{prefix}", getConfig().getString("prefix", "")));
    }

    private String msg(String key, String placeholder, String replacement) {
        return msg(key).replace(placeholder, replacement);
    }

    private void cancelCountdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        countingDown = false;
    }

    private void scheduleDailyRestart() {
        ZonedDateTime now = ZonedDateTime.now(MSK);
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(MSK).plusDays(1);

        long secondsUntil = Duration.between(now, midnight).getSeconds();

        if (secondsUntil > 300) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    Bukkit.broadcastMessage(msg("five-minutes"))
            , (secondsUntil - 300) * 20);
        } else {
            Bukkit.broadcastMessage(msg("less-five-minutes"));
        }

        if (secondsUntil > 60) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    startCountdown(60, msg("bossbar-planned"))
            , (secondsUntil - 60) * 20);
        } else {
            startCountdown((int) secondsUntil, msg("bossbar-planned"));
        }

        Bukkit.getScheduler().runTaskLater(this, this::restartServer, secondsUntil * 20);
    }

    private void startCountdown(int totalSeconds, String reason) {
        cancelCountdown();
        countingDown = true;
        countdownSeconds = totalSeconds;

        bossBar = Bukkit.createBossBar(reason, BarColor.RED, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (countdownSeconds <= 0) {
                cancelCountdown();
                return;
            }

            countdownSeconds--;
            double progress = (double) countdownSeconds / totalSeconds;
            bossBar.setProgress(Math.max(0.0, progress));
            bossBar.setTitle(reason + " §c" + formatTime(countdownSeconds));

            if (countdownSeconds <= 10 && countdownSeconds > 0) {
                String title = msg("title", "{seconds}", String.valueOf(countdownSeconds));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("", title, 0, 20, 5);
                }
            }

            if (countdownSeconds == 10) {
                Bukkit.broadcastMessage(msg("ten-seconds"));
            }
        }, 0L, 20L);
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (mins > 0) {
            return mins + "м " + secs + "с";
        }
        return secs + "с";
    }

    private void restartServer() {
        Bukkit.broadcastMessage(msg("restarting"));
        Bukkit.getScheduler().runTaskLater(this, () ->
                Bukkit.getServer().shutdown()
        , 20L);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("autorestart.restart")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }

        if (countingDown) {
            sender.sendMessage(msg("already-scheduled"));
            return true;
        }

        Bukkit.broadcastMessage(msg("command-1"));
        Bukkit.broadcastMessage(msg("command-2"));
        startCountdown(60, msg("bossbar-unscheduled"));
        Bukkit.getScheduler().runTaskLater(this, this::restartServer, 60 * 20);
        return true;
    }
}
