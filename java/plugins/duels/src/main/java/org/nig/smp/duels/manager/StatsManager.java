package org.nig.smp.duels.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.model.PlayerStats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StatsManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private File file;

    public StatsManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stats.clear();
        file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerStats playerStats = new PlayerStats(
                    section.getInt(key + ".wins", 0),
                    section.getInt(key + ".losses", 0),
                    section.getInt(key + ".winstreak", 0),
                    section.getInt(key + ".best-winstreak", 0)
                );
                stats.put(uuid, playerStats);
            } catch (IllegalArgumentException ignored) {
            }
        }
        plugin.getLogger().info("Загружена статистика дуэлей: " + stats.size() + " игроков");
    }

    public PlayerStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public void recordWin(UUID uuid) {
        getStats(uuid).recordWin();
    }

    public void recordLoss(UUID uuid) {
        getStats(uuid).recordLoss();
    }

    public void save() {
        if (file == null) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("players");
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            PlayerStats s = entry.getValue();
            String key = entry.getKey().toString();
            section.set(key + ".wins", s.getWins());
            section.set(key + ".losses", s.getLosses());
            section.set(key + ".winstreak", s.getWinstreak());
            section.set(key + ".best-winstreak", s.getBestWinstreak());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить stats.yml: " + ex.getMessage());
        }
    }
}
