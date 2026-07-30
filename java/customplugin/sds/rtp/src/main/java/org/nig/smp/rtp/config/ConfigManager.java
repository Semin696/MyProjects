package org.nig.smp.rtp.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private int minY;
    private int maxY;

    private String prefix;
    private String messageTeleporting;
    private String messageSuccess;
    private String messageCooldown;
    private String messageNoPermission;
    private String messageWorldDisabled;
    private String messageSafeLocationNotFound;

    private int cooldownSeconds;
    private boolean useWorldBorder;
    private java.util.List<String> enabledWorlds;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        minX = config.getInt("rtp.min-x", -5000);
        maxX = config.getInt("rtp.max-x", 5000);
        minZ = config.getInt("rtp.min-z", -5000);
        maxZ = config.getInt("rtp.max-z", 5000);
        minY = config.getInt("rtp.min-y", -64);
        maxY = config.getInt("rtp.max-y", 320);
        cooldownSeconds = config.getInt("rtp.cooldown-seconds", 0);
        useWorldBorder = config.getBoolean("rtp.use-world-border", false);
        enabledWorlds = config.getStringList("rtp.enabled-worlds");

        prefix = color(config.getString("messages.prefix", "&8[&c&lmcru&8]"));
        messageTeleporting = color(config.getString("messages.teleporting", "&aTeleporting to a random location..."));
        messageSuccess = color(config.getString("messages.success", "&aYou have been teleported to &e%x% &a/ &e%z%"));
        messageCooldown = color(config.getString("messages.cooldown", "&cYou must wait &e%time% &cseconds before using /rtp again"));
        messageNoPermission = color(config.getString("messages.no-permission", "&cYou do not have permission to use this command"));
        messageWorldDisabled = color(config.getString("messages.world-disabled", "&cRTP is disabled in this world"));
        messageSafeLocationNotFound = color(config.getString("messages.safe-location-not-found", "&cCould not find a safe location. Try again"));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public boolean isUseWorldBorder() { return useWorldBorder; }
    public java.util.List<String> getEnabledWorlds() { return enabledWorlds; }

    public String prefixed(String msg) {
        return prefix + " " + msg;
    }

    public String getMessageTeleporting() { return prefixed(messageTeleporting); }
    public String getMessageSuccess() { return prefixed(messageSuccess); }
    public String getMessageCooldown() { return prefixed(messageCooldown); }
    public String getMessageNoPermission() { return prefixed(messageNoPermission); }
    public String getMessageWorldDisabled() { return prefixed(messageWorldDisabled); }
    public String getMessageSafeLocationNotFound() { return prefixed(messageSafeLocationNotFound); }
}
