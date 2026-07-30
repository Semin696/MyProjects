package org.nig.smp.spawn;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Spawn extends JavaPlugin {

    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadMessages();
        getLogger().info("Spawn plugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Spawn plugin disabled");
    }

    private void reloadMessages() {
        reloadConfig();
        prefix = color(getConfig().getString("prefix", "&8[&6Spawn&8]&r"));
    }

    private String msg(String key) {
        String raw = getConfig().getString("messages." + key, "&cMessage not found: " + key);
        return color(raw.replace("{prefix}", prefix));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("no-player"));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "setspawn" -> {
                if (!player.hasPermission("spawn.setspawn")) {
                    player.sendMessage(msg("no-permission"));
                    return true;
                }
                Location loc = player.getLocation();
                FileConfiguration config = getConfig();
                config.set("spawn.world", loc.getWorld().getName());
                config.set("spawn.x", loc.getX());
                config.set("spawn.y", loc.getY());
                config.set("spawn.z", loc.getZ());
                config.set("spawn.yaw", (double) loc.getYaw());
                config.set("spawn.pitch", (double) loc.getPitch());
                saveConfig();
                player.sendMessage(msg("spawn-set"));
                return true;
            }
            case "spawn" -> {
                if (!player.hasPermission("spawn.spawn")) {
                    player.sendMessage(msg("no-permission"));
                    return true;
                }
                FileConfiguration config = getConfig();
                if (!config.contains("spawn.world")) {
                    player.sendMessage(msg("spawn-not-set"));
                    return true;
                }
                Location loc = new Location(
                        getServer().getWorld(config.getString("spawn.world")),
                        config.getDouble("spawn.x"),
                        config.getDouble("spawn.y"),
                        config.getDouble("spawn.z"),
                        (float) config.getDouble("spawn.yaw"),
                        (float) config.getDouble("spawn.pitch")
                );
                player.teleport(loc);
                player.sendMessage(msg("teleported"));
                return true;
            }
        }
        return false;
    }
}
