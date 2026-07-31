package pl.ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
        if (plugin.equals(this)) {
            getLogger().info("Попытка перезагрузить PlReloader отклонена.");
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

    private String getPluginNameFromFile(File pluginFile) {
        try (ZipFile zip = new ZipFile(pluginFile)) {
            ZipEntry entry = zip.getEntry("plugin.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(in);
                return desc.getName();
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Не удалось прочитать plugin.yml из " + pluginFile.getName(), e);
            return null;
        }
    }

    private Plugin loadNewFromFile(File pluginFile) {
        try {
            Plugin loaded = Bukkit.getPluginManager().loadPlugin(pluginFile);
            if (loaded != null) {
                loaded.onLoad();
                Bukkit.getPluginManager().enablePlugin(loaded);
            }
            return loaded;
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            getLogger().log(Level.WARNING, "Не удалось загрузить плагин: " + pluginFile.getName(), e);
            return null;
        }
    }

    public Plugin loadFromFile(String fileName) {
        File pluginsDir = new File("plugins");
        File pluginFile = new File(pluginsDir, fileName);
        if (!pluginFile.exists()) {
            pluginFile = new File(pluginsDir, fileName + ".jar");
        }
        if (!pluginFile.exists()) {
            return null;
        }

        String name = getPluginNameFromFile(pluginFile);
        if (name != null && name.equals(getName())) {
            getLogger().info("Попытка загрузить PlReloader заново отклонена.");
            return null;
        }
        if (name != null) {
            Plugin existing = Bukkit.getPluginManager().getPlugin(name);
            if (existing != null) {
                Bukkit.getPluginManager().disablePlugin(existing);
            }
        }

        return loadNewFromFile(pluginFile);
    }

    public List<Plugin> loadAllNew() {
        List<Plugin> loaded = new ArrayList<>();
        File pluginsDir = new File("plugins");
        File[] jars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) {
            return loaded;
        }
        for (File jar : jars) {
            String name = getPluginNameFromFile(jar);
            if (name == null) {
                continue;
            }
            if (name.equals(getName())) {
                continue;
            }
            Plugin existing = Bukkit.getPluginManager().getPlugin(name);
            if (existing != null) {
                Bukkit.getPluginManager().disablePlugin(existing);
            }
            Plugin plugin = loadNewFromFile(jar);
            if (plugin != null) {
                loaded.add(plugin);
            }
        }
        return loaded;
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
        if (plugin.equals(this)) {
            getLogger().info("Попытка выключить PlReloader отклонена.");
            return;
        }
        if (plugin.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }
}
