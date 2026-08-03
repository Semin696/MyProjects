package org.nig.smp.duels;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.duels.cmi.CMIKitBridge;
import org.nig.smp.duels.command.DuelCommand;
import org.nig.smp.duels.command.KitCommand;
import org.nig.smp.duels.listener.PlayerListener;
import org.nig.smp.duels.manager.ArenaManager;
import org.nig.smp.duels.manager.DuelManager;
import org.nig.smp.duels.manager.VisibilityManager;

import java.util.UUID;

public final class DuelsPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private VisibilityManager visibilityManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getDuelsWorld();

        this.arenaManager = new ArenaManager(this);
        arenaManager.load();

        this.visibilityManager = new VisibilityManager(this);
        this.duelManager = new DuelManager(this, arenaManager, visibilityManager);

        DuelCommand duelCommand = new DuelCommand(this, duelManager);
        getCommand("duel").setExecutor(duelCommand);
        getCommand("duel").setTabCompleter(duelCommand);
        getCommand("kit").setExecutor(new KitCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this, duelManager, visibilityManager), this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for (UUID uuid : duelManager.getWaitingPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendActionBar(msg("waiting-actionbar"));
                }
            }
        }, 0L, 20L);

        if (!CMIKitBridge.isAvailable()) {
            getLogger().warning("Плагин CMI не найден! Киты для дуэлей будут недоступны.");
        }

        getLogger().info("Duels enabled");
    }

    @Override
    public void onDisable() {
        if (duelManager != null) {
            duelManager.shutdown();
        }
        getLogger().info("Duels disabled");
    }

    public World getDuelsWorld() {
        String name = getConfig().getString("world-name", "duels");
        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldCreator creator = WorldCreator.name(name);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("2;0;1");
            world = Bukkit.createWorld(creator);
        }
        if (world != null) {
            world.setPVP(true);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setTime(6000);
        }
        return world;
    }

    public GameMode getDuelsGameMode() {
        String mode = getConfig().getString("duels-gamemode", "SURVIVAL");
        try {
            return GameMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.SURVIVAL;
        }
    }

    public boolean isDuelsWorld(World world) {
        World duels = getDuelsWorld();
        return duels != null && duels.getName().equals(world.getName());
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public VisibilityManager getVisibilityManager() {
        return visibilityManager;
    }

    public String raw(String path, Object... placeholders) {
        String text = getConfig().getString("messages." + path);
        if (text == null) {
            return "Сообщение не найдено: " + path;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public Component msg(String path, Object... placeholders) {
        return LegacyComponentSerializer.legacySection().deserialize(raw(path, placeholders));
    }
}
