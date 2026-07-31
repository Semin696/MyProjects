package org.nig.smp.impers.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.impers.ImpersPlugin;
import org.nig.smp.impers.model.Territory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerritoryManager {

    private final ImpersPlugin plugin;
    private final Map<String, Territory> territories = new HashMap<>();
    private final File file;

    public TerritoryManager(ImpersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "territories.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("territories");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(key);
            UUID owner = t.contains("owner") ? UUID.fromString(t.getString("owner")) : null;
            Territory territory = new Territory(
                t.getString("name"),
                t.getString("tag"),
                t.getString("world"),
                t.getInt("minChunkX"),
                t.getInt("minChunkZ"),
                t.getInt("maxChunkX"),
                t.getInt("maxChunkZ"),
                owner
            );
            for (String member : t.getStringList("members")) {
                territory.addMember(UUID.fromString(member));
            }
            territories.put(key.toLowerCase(), territory);
        }
        plugin.getLogger().info("Loaded " + territories.size() + " territories");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Territory t : territories.values()) {
            String path = "territories." + t.getName().toLowerCase();
            config.set(path + ".name", t.getName());
            config.set(path + ".tag", t.getTag());
            config.set(path + ".world", t.getWorld());
            config.set(path + ".minChunkX", t.getMinChunkX());
            config.set(path + ".minChunkZ", t.getMinChunkZ());
            config.set(path + ".maxChunkX", t.getMaxChunkX());
            config.set(path + ".maxChunkZ", t.getMaxChunkZ());
            if (t.getOwner() != null) {
                config.set(path + ".owner", t.getOwner().toString());
            }
            config.set(path + ".members", t.getMembers().stream().map(UUID::toString).toList());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save territories: " + e.getMessage());
        }
    }

    public void add(Territory territory) {
        territories.put(territory.getName().toLowerCase(), territory);
        save();
    }

    public void remove(String name) {
        territories.remove(name.toLowerCase());
        save();
    }

    public Territory get(String name) {
        return territories.get(name.toLowerCase());
    }

    public Territory getTerritoryAt(String world, int chunkX, int chunkZ) {
        for (Territory t : territories.values()) {
            if (t.containsChunk(world, chunkX, chunkZ)) {
                return t;
            }
        }
        return null;
    }

    public Collection<Territory> getAll() {
        return territories.values();
    }

    public boolean exists(String name) {
        return territories.containsKey(name.toLowerCase());
    }
}
