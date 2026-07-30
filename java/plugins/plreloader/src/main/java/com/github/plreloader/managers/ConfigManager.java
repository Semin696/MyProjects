package com.github.plreloader.managers;

import com.github.plreloader.PluginReloader;
import org.bukkit.ChatColor;

public class ConfigManager {

    private final PluginReloader plugin;
    private String prefix;

    public ConfigManager(PluginReloader plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("prefix", "&7[&bPLReloader&7] &f"));
    }

    public String getPrefix() {
        return prefix;
    }
}
