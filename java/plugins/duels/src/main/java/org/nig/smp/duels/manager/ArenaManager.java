package org.nig.smp.duels.manager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.model.Arena;

import java.util.ArrayList;
import java.util.List;

public final class ArenaManager {

    private final DuelsPlugin plugin;
    private final List<Arena> arenas = new ArrayList<>();

    public ArenaManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        arenas.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("arenas");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (!section.getBoolean(key + ".enabled", true)) {
                continue;
            }
            Location spawn1 = parseLocation(section.getConfigurationSection(key + ".spawn1"));
            Location spawn2 = parseLocation(section.getConfigurationSection(key + ".spawn2"));
            if (spawn1 == null || spawn2 == null) {
                plugin.getLogger().warning("Арена " + key + " имеет неверные координаты спавнов, пропущена");
                continue;
            }
            String display = section.getString(key + ".display-name");
            if (display == null || display.isEmpty()) {
                display = key;
            }
            boolean allowBlockBreak = section.getBoolean(key + ".allow-block-break", false);
            arenas.add(new Arena(key, ChatColor.translateAlternateColorCodes('&', display), allowBlockBreak, spawn1, spawn2));
        }
        plugin.getLogger().info("Загружено арен: " + arenas.size());
    }

    private Location parseLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = plugin.getDuelsWorld();
        if (world == null) {
            return null;
        }
        Location loc = new Location(
            world,
            section.getDouble("x", 0.0),
            section.getDouble("y", 64.0),
            section.getDouble("z", 0.0)
        );
        loc.setYaw((float) section.getDouble("yaw", 0.0));
        loc.setPitch((float) section.getDouble("pitch", 0.0));
        return loc;
    }

    public List<Arena> getArenas() {
        return arenas;
    }

    public List<String> getArenaNames() {
        List<String> names = new ArrayList<>();
        for (Arena arena : arenas) {
            names.add(arena.getName());
        }
        return names;
    }

    public Arena getArena(String name) {
        for (Arena arena : arenas) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }
}
