package pl.ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class PlReloader extends JavaPlugin {

    private static PlReloader instance;
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadPrefix();

        getCommand("plreload").setExecutor(new PlReloaderCommand());

        getLogger().info("PlReloader включён! Используйте /plreload help для списка команд.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlReloader выключен.");
    }

    public void reloadPrefix() {
        reloadConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&8[&bPlReloader&8] &7"));
    }

    public String getPrefix() {
        return prefix;
    }

    public static PlReloader getInstance() {
        return instance;
    }

    public String getMessage(String key) {
        return prefix + ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + key, "&f" + key));
    }

    public void reloadPlugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return;
        }

        Bukkit.getPluginManager().disablePlugin(plugin);

        File pluginFile = new File("plugins", plugin.getName() + ".jar");
        if (!pluginFile.exists()) {
            pluginFile = new File("plugins", plugin.getName() + ".jar");
            File pluginsDir = new File("plugins");
            File[] files = pluginsDir.listFiles((dir, name) -> name.startsWith(plugin.getName()) && name.endsWith(".jar"));
            if (files != null && files.length > 0) {
                pluginFile = files[0];
            } else {
                return;
            }
        }

        try {
            Plugin loadedPlugin = Bukkit.getPluginManager().loadPlugin(pluginFile);
            if (loadedPlugin != null) {
                loadedPlugin.onLoad();
                Bukkit.getPluginManager().enablePlugin(loadedPlugin);
            }
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            getLogger().log(Level.WARNING, "Не удалось перезагрузить плагин: " + pluginName, e);
        }
    }

    public void enablePlugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return;
        }
        if (!plugin.isEnabled()) {
            Bukkit.getPluginManager().enablePlugin(plugin);
        }
    }

    public void disablePlugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null) {
            return;
        }
        if (plugin.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
}
