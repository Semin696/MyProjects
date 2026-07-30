package org.nig.smp.homeSev.manager;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.nig.smp.homeSev.HomeSev;
import org.nig.smp.homeSev.model.Home;
import org.nig.smp.homeSev.model.Home.Type;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HomeManager {

    private final HomeSev plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<UUID, Map<String, Home>> homesByPlayer = new HashMap<>();

    public HomeManager(HomeSev plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "homes.yml");
        loadHomes();
    }

    private void loadHomes() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection playersSection = data.getConfigurationSection("homes");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection playerHomes = playersSection.getConfigurationSection(uuidStr);
            if (playerHomes == null) continue;

            Map<String, Home> homes = new HashMap<>();
            for (String homeName : playerHomes.getKeys(false)) {
                ConfigurationSection homeSection = playerHomes.getConfigurationSection(homeName);
                if (homeSection != null) {
                    Home home = Home.loadFromConfig(homeName, homeSection);
                    homes.put(homeName, home);
                }
            }
            homesByPlayer.put(uuid, homes);
        }
    }

    public void saveHomes() {
        data.set("homes", null);

        for (Map.Entry<UUID, Map<String, Home>> entry : homesByPlayer.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Home> homeEntry : entry.getValue().entrySet()) {
                String path = "homes." + uuidStr + "." + homeEntry.getKey();
                ConfigurationSection section = data.createSection(path);
                homeEntry.getValue().saveToConfig(section);
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public Map<String, Home> getPlayerHomes(Player player) {
        return homesByPlayer.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    public Home getHome(Player player, String name) {
        Map<String, Home> homes = homesByPlayer.get(player.getUniqueId());
        if (homes == null) return null;
        return homes.get(name);
    }

    public void setHome(Player player, String name, Location location, Type type) {
        UUID uuid = player.getUniqueId();
        homesByPlayer.computeIfAbsent(uuid, k -> new HashMap<>());
        Home home = Home.fromLocation(name, location, type);
        homesByPlayer.get(uuid).put(name, home);
        saveHomes();
    }

    public boolean removeHome(Player player, String name) {
        Map<String, Home> homes = homesByPlayer.get(player.getUniqueId());
        if (homes == null) return false;
        Home removed = homes.remove(name);
        if (removed != null) {
            saveHomes();
            return true;
        }
        return false;
    }

    public int getStandardHomeCount(Player player) {
        return (int) getPlayerHomes(player).values().stream()
            .filter(h -> h.getType() == Type.STANDARD)
            .count();
    }

    public int getCommandHomeCount(Player player) {
        return (int) getPlayerHomes(player).values().stream()
            .filter(h -> h.getType() == Type.COMMAND)
            .count();
    }

    public int getStandardLimit(Player player) {
        int configLimit = plugin.getConfig().getInt("standard-limit", 2);
        int permissionLimit = getMaxPermissionValue(player, "homesev.limit.standard.");
        return Math.max(configLimit, permissionLimit);
    }

    public int getCommandLimit(Player player) {
        int configLimit = plugin.getConfig().getInt("command-limit", 1);
        int permissionLimit = getMaxPermissionValue(player, "homesev.limit.command.");
        return Math.max(configLimit, permissionLimit);
    }

    private int getMaxPermissionValue(Player player, String prefix) {
        int max = 0;
        for (var info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith(prefix)) {
                String suffix = perm.substring(prefix.length());
                try {
                    int value = Integer.parseInt(suffix);
                    if (value > max) max = value;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    public boolean isCommandHomeName(String name) {
        return plugin.getConfig().getStringList("command-home-names").contains(name.toLowerCase());
    }

    public List<String> getHomeNames(Player player) {
        return new ArrayList<>(getPlayerHomes(player).keySet());
    }
}
