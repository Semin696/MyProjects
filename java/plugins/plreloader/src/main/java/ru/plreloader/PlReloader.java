package ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class PlReloader extends JavaPlugin {

    private static PlReloader instance;
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadPrefix();

        getCommand("plreload").setExecutor(new ReloadCommand());
        getCommand("plreloadall").setExecutor(new ReloadCommand());
        getCommand("plenable").setExecutor(new ReloadCommand());
        getCommand("pldisable").setExecutor(new ReloadCommand());
        getCommand("pllist").setExecutor(new ReloadCommand());

        getLogger().info("PlReloader включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlReloader выключен!");
    }

    public void loadPrefix() {
        prefix = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("prefix", "&7[&6PlReloader&7] &r"));
    }

    public static PlReloader getInstance() {
        return instance;
    }

    public String getPrefix() {
        return prefix;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
