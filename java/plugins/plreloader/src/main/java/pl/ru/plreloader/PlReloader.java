package pl.ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        File pluginFile = findPluginFile(plugin.getName());
        if (pluginFile == null) {
            getLogger().warning("Не найден jar-файл для плагина: " + pluginName);
            return;
        }

        unloadPlugin(plugin);

        Plugin loadedPlugin = loadNewFromFile(pluginFile);
        if (loadedPlugin != null) {
            getLogger().info("Плагин перезагружен: " + loadedPlugin.getName());
        } else {
            getLogger().warning("Не удалось перезагрузить плагин: " + pluginName);
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
                unloadPlugin(existing);
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
                unloadPlugin(existing);
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
        unloadPlugin(plugin);
    }

    private File findPluginFile(String pluginName) {
        File pluginsDir = new File("plugins");
        File direct = new File(pluginsDir, pluginName + ".jar");
        if (direct.exists()) {
            return direct;
        }
        File[] files = pluginsDir.listFiles((dir, name) -> name.startsWith(pluginName) && name.endsWith(".jar"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
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
                Bukkit.getPluginManager().enablePlugin(loaded);
            }
            return loaded;
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            getLogger().log(Level.WARNING, "Не удалось загрузить плагин: " + pluginFile.getName(), e);
            return null;
        }
    }

    private void unloadPlugin(Plugin plugin) {
        try {
            unregisterCommands(plugin);
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Не удалось удалить команды плагина " + plugin.getName(), t);
        }

        if (plugin.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }

        try {
            SimplePluginManager pm = (SimplePluginManager) Bukkit.getPluginManager();

            Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Plugin> plugins = (List<Plugin>) pluginsField.get(pm);
            plugins.remove(plugin);

            Field lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(pm);
            lookupNames.entrySet().removeIf(e -> e.getValue() == plugin || e.getValue().equals(plugin));
            lookupNames.values().removeIf(p -> p == plugin);

            if (plugin instanceof JavaPlugin) {
                ClassLoader cl = plugin.getClass().getClassLoader();
                if (cl instanceof PluginClassLoader) {
                    ((PluginClassLoader) cl).close();
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Не удалось полностью выгрузить плагин: " + plugin.getName(), e);
        }
    }

    private void unregisterCommands(Plugin plugin) {
        if (plugin.getDescription().getCommands() == null || plugin.getDescription().getCommands().isEmpty()) {
            return;
        }
        CommandMap commandMap = Bukkit.getCommandMap();
        if (!(commandMap instanceof SimpleCommandMap)) {
            return;
        }
        SimpleCommandMap simple = (SimpleCommandMap) commandMap;
        Map<String, Command> known = simple.getKnownCommands();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Command> entry : known.entrySet()) {
            Command cmd = entry.getValue();
            if (cmd instanceof PluginCommand) {
                PluginCommand pc = (PluginCommand) cmd;
                if (plugin.equals(pc.getPlugin())) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        for (String name : plugin.getDescription().getCommands().keySet()) {
            Command cmd = known.get(name);
            if (cmd != null && !toRemove.contains(name)) {
                toRemove.add(name);
            }
        }

        for (String key : toRemove) {
            Command cmd = known.remove(key);
            if (cmd != null) {
                cmd.unregister(commandMap);
            }
        }
    }
}
