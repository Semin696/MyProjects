package com.github.plreloader;

import com.github.plreloader.commands.ReloadCommand;
import com.github.plreloader.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginReloader extends JavaPlugin {

    private static PluginReloader instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        getCommand("plreload").setExecutor(new ReloadCommand(this));
        getLogger().info("PLReloader включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PLReloader выключен!");
    }

    public static PluginReloader getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
