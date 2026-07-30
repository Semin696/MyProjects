package org.nig.smp.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SettingsManager {

    private final Settings plugin;
    private final File dataFile;
    private YamlConfiguration config;
    private final Map<UUID, Map<SettingType, Boolean>> playerSettings = new HashMap<>();

    public SettingsManager(Settings plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "settings.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create settings.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<SettingType, Boolean> settings = new EnumMap<>(SettingType.class);
                for (SettingType type : SettingType.values()) {
                    if (config.isSet(uuidStr + "." + type.getKey())) {
                        settings.put(type, config.getBoolean(uuidStr + "." + type.getKey()));
                    }
                }
                if (!settings.isEmpty()) {
                    playerSettings.put(uuid, settings);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        plugin.getLogger().info("Loaded " + playerSettings.size() + " player settings");
    }

    public void save() {
        for (String key : config.getKeys(false)) {
            try {
                if (!playerSettings.containsKey(UUID.fromString(key))) {
                    config.set(key, null);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        for (Map.Entry<UUID, Map<SettingType, Boolean>> entry : playerSettings.entrySet()) {
            String path = entry.getKey().toString();
            for (Map.Entry<SettingType, Boolean> setting : entry.getValue().entrySet()) {
                config.set(path + "." + setting.getKey().getKey(), setting.getValue());
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save settings: " + e.getMessage());
        }
    }

    public boolean isEnabled(Player player, SettingType type) {
        Map<SettingType, Boolean> settings = playerSettings.get(player.getUniqueId());
        return settings != null && settings.getOrDefault(type, false);
    }

    public void setEnabled(Player player, SettingType type, boolean enabled) {
        playerSettings.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(SettingType.class))
                .put(type, enabled);
        save();
    }

    public boolean toggle(Player player, SettingType type) {
        boolean newState = !isEnabled(player, type);
        setEnabled(player, type, newState);
        return newState;
    }

    public Map<SettingType, Boolean> getSettings(Player player) {
        return playerSettings.getOrDefault(player.getUniqueId(), new EnumMap<>(SettingType.class));
    }
}
