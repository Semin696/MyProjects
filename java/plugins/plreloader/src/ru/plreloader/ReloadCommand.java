package ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final PLReloader plugin;

    public ReloadCommand(PLReloader plugin) {
        this.plugin = plugin;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getPrefix() + msg);
    }

    private String colorRaw(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("plreloader.admin")) {
            sender.sendMessage(color("&cУ вас нет прав на использование этой команды!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&eИспользование: /plreload <reload|load|unload|list|reloadall> [plugin]"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender, args);
            case "load":
                return handleLoad(sender, args);
            case "unload":
                return handleUnload(sender, args);
            case "list":
                return handleList(sender);
            case "reloadall":
                return handleReloadAll(sender);
            case "reloadconfig":
                return handleReloadConfig(sender);
            default:
                sender.sendMessage(color("&cНеизвестная подкоманда. Используйте: reload, load, unload, list, reloadall"));
                return true;
        }
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cУкажите название плагина. Пример: /plreload reload Essentials"));
            return true;
        }

        String pluginName = args[1];
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(color("&cПлагин с именем \"" + pluginName + "\" не найден!"));
            return true;
        }

        if (target.equals(plugin)) {
            sender.sendMessage(color("&cНельзя перезагрузить PLReloader через самого себя! Используйте /plreload reloadconfig"));
            return true;
        }

        try {
            reloadPlugin(target);
            sender.sendMessage(color("&aПлагин \"" + target.getName() + "\" успешно перезагружен!"));
        } catch (Exception e) {
            sender.sendMessage(color("&cОшибка при перезагрузке плагина: " + e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cУкажите название jar-файла. Пример: /plreload load MyPlugin.jar"));
            return true;
        }

        String fileName = args[1];
        if (!fileName.endsWith(".jar")) {
            fileName += ".jar";
        }

        try {
            loadPlugin(fileName);
            sender.sendMessage(color("&aПлагин \"" + fileName + "\" успешно загружен!"));
        } catch (Exception e) {
            sender.sendMessage(color("&cОшибка при загрузке плагина: " + e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cУкажите название плагина. Пример: /plreload unload Essentials"));
            return true;
        }

        String pluginName = args[1];
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(color("&cПлагин с именем \"" + pluginName + "\" не найден!"));
            return true;
        }

        if (target.equals(plugin)) {
            sender.sendMessage(color("&cНельзя выгрузить PLReloader!"));
            return true;
        }

        try {
            unloadPlugin(target);
            sender.sendMessage(color("&aПлагин \"" + target.getName() + "\" успешно выгружен!"));
        } catch (Exception e) {
            sender.sendMessage(color("&cОшибка при выгрузке плагина: " + e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        PluginManager pm = Bukkit.getPluginManager();
        Plugin[] plugins = pm.getPlugins();

        StringBuilder sb = new StringBuilder();
        sb.append(color("&6Список плагинов (&e").append(plugins.length).append("&6):\n"));

        for (Plugin p : plugins) {
            String status = p.isEnabled() ? "&a✔" : "&c✘";
            String name = p.getName();
            String version = p.getDescription() != null && p.getDescription().getVersion() != null
                    ? p.getDescription().getVersion() : "?";
            sb.append(colorRaw(" &8- " + status + " &f" + name + " &7v" + version + "\n"));
        }

        sender.sendMessage(sb.toString().trim());
        return true;
    }

    private boolean handleReloadAll(CommandSender sender) {
        PluginManager pm = Bukkit.getPluginManager();
        Plugin[] plugins = pm.getPlugins();

        int count = 0;
        for (Plugin p : plugins) {
            if (p.equals(plugin)) continue;
            try {
                reloadPlugin(p);
                count++;
            } catch (Exception e) {
                sender.sendMessage(color("&cОшибка при перезагрузке \"" + p.getName() + "\": " + e.getMessage()));
            }
        }

        sender.sendMessage(color("&aПерезагружено плагинов: " + count));
        return true;
    }

    private boolean handleReloadConfig(CommandSender sender) {
        plugin.reloadPrefix();
        sender.sendMessage(color("&aКонфиг PLReloader перезагружен!"));
        return true;
    }

    private void reloadPlugin(Plugin target) throws Exception {
        PluginManager pm = Bukkit.getPluginManager();
        File pluginFile = getPluginFile(target);

        pm.disablePlugin(target);
        unloadPluginFromMaps(target);

        if (pluginFile != null && pluginFile.exists()) {
            Plugin loaded = pm.loadPlugin(pluginFile);
            if (loaded != null) {
                pm.enablePlugin(loaded);
            } else {
                throw new Exception("Не удалось загрузить плагин из файла " + pluginFile.getName());
            }
        } else {
            throw new Exception("Файл плагина не найден: " + target.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void unloadPlugin(Plugin target) throws Exception {
        PluginManager pm = Bukkit.getPluginManager();
        pm.disablePlugin(target);
        unloadPluginFromMaps(target);
    }

    @SuppressWarnings("unchecked")
    private void unloadPluginFromMaps(Plugin target) throws Exception {
        PluginManager pm = Bukkit.getPluginManager();

        Field pluginsField = pm.getClass().getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        List<Plugin> plugins = (List<Plugin>) pluginsField.get(pm);
        plugins.remove(target);

        Field lookupNamesField = pm.getClass().getDeclaredField("lookupNames");
        lookupNamesField.setAccessible(true);
        Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(pm);
        lookupNames.values().remove(target);
    }

    private void loadPlugin(String fileName) throws Exception {
        PluginManager pm = Bukkit.getPluginManager();
        File pluginsDir = new File("plugins");
        File pluginFile = new File(pluginsDir, fileName);

        if (!pluginFile.exists()) {
            throw new Exception("Файл \"" + fileName + "\" не найден в папке plugins!");
        }

        Plugin loaded = pm.loadPlugin(pluginFile);
        if (loaded != null) {
            pm.enablePlugin(loaded);
        } else {
            throw new Exception("Не удалось загрузить плагин из файла " + fileName);
        }
    }

    private File getPluginFile(Plugin target) {
        if (target instanceof JavaPlugin) {
            try {
                Field fileField = JavaPlugin.class.getDeclaredField("file");
                fileField.setAccessible(true);
                return (File) fileField.get(target);
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("plreloader.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "load", "unload", "list", "reloadall", "reloadconfig");
            return subcommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("unload")) {
                return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                        .map(Plugin::getName)
                        .filter(name -> !name.equals("PLReloader"))
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("load")) {
                File pluginsDir = new File("plugins");
                File[] jars = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jars != null) {
                    return Arrays.stream(jars)
                            .map(File::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }
}
