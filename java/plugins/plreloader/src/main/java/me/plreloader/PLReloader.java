package me.plreloader;

import me.plreloader.commands.DisableCommand;
import me.plreloader.commands.EnableCommand;
import me.plreloader.commands.ListCommand;
import me.plreloader.commands.ReloadAllCommand;
import me.plreloader.commands.ReloadCommand;
import me.plreloader.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PLReloader extends JavaPlugin {

    private static PLReloader instance;
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        registerCommands();
        getLogger().info("PLReloader включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PLReloader выключен.");
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        prefix = getConfig().getString("prefix", "&7[&bPLReloader&7] &r");
    }

    private void registerCommands() {
        getCommand("plreload").setExecutor(new ReloadCommand());
        getCommand("plenable").setExecutor(new EnableCommand());
        getCommand("pldisable").setExecutor(new DisableCommand());
        getCommand("pllist").setExecutor(new ListCommand());
        getCommand("plreloadall").setExecutor(new ReloadAllCommand());
    }

    public void reloadPluginConfig() {
        loadConfig();
    }

    public static PLReloader getInstance() {
        return instance;
    }

    public String getPref() {
        return prefix;
    }

    public Component getMessage(String path) {
        String msg = getConfig().getString("messages." + path, "&cСообщение не найдено: " + path);
        return MessageUtil.colorize(prefix + msg);
    }

    public Component getMessage(String path, String placeholder, String value) {
        String msg = getConfig().getString("messages." + path, "&cСообщение не найдено: " + path);
        msg = MessageUtil.format(msg, placeholder, value);
        return MessageUtil.colorize(prefix + msg);
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(getMessage("no-permission"));
            return false;
        }
        return true;
    }
}
