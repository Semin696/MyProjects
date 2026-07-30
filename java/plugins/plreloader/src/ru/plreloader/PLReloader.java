package ru.plreloader;

import org.bukkit.plugin.java.JavaPlugin;

public class PLReloader extends JavaPlugin {

    private static PLReloader instance;
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        prefix = getConfig().getString("prefix", "&8[&bPLReloader&8] &7");

        this.getCommand("plreload").setExecutor(new ReloadCommand(this));

        getLogger().info("PLReloader включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PLReloader выключен!");
    }

    public static PLReloader getInstance() {
        return instance;
    }

    public String getPrefix() {
        return prefix;
    }

    public void reloadPrefix() {
        reloadConfig();
        prefix = getConfig().getString("prefix", "&8[&bPLReloader&8] &7");
    }
}
