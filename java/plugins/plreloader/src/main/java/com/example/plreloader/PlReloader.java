package com.example.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PlReloader extends JavaPlugin implements TabExecutor {

    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix", "&7[&6PlReloader&7]"));

        if (getCommand("plreload") != null) {
            getCommand("plreload").setExecutor(this);
            getCommand("plreload").setTabCompleter(this);
        }

        getLogger().info("Плагин включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин выключен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("plreloader.admin")) {
            sender.sendMessage(prefix + " §cУ вас нет прав на использование этой команды.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(prefix + " §cИспользование: /" + label + " <reload|enable|disable|list> [plugin]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender, args);
            case "enable":
                return handleEnable(sender, args);
            case "disable":
                return handleDisable(sender, args);
            case "list":
                return handleList(sender);
            default:
                sender.sendMessage(prefix + " §cНеизвестная подкоманда. Используйте: reload, enable, disable, list");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + " §cУкажите название плагина. Пример: /plreload reload Essentials");
            return true;
        }

        String pluginName = args[1];
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        if (plugin == null) {
            sender.sendMessage(prefix + " §cПлагин \"" + pluginName + "\" не найден.");
            return true;
        }

        if (plugin == this) {
            sender.sendMessage(prefix + " §cНельзя перезагрузить PlReloader через самого себя.");
            return true;
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.disablePlugin(plugin);
        pm.enablePlugin(plugin);

        sender.sendMessage(prefix + " §aПлагин \"" + pluginName + "\" успешно перезагружен.");
        return true;
    }

    private boolean handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + " §cУкажите название плагина. Пример: /plreload enable Essentials");
            return true;
        }

        String pluginName = args[1];
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        if (plugin == null) {
            sender.sendMessage(prefix + " §cПлагин \"" + pluginName + "\" не найден.");
            return true;
        }

        if (plugin.isEnabled()) {
            sender.sendMessage(prefix + " §eПлагин \"" + pluginName + "\" уже включён.");
            return true;
        }

        Bukkit.getPluginManager().enablePlugin(plugin);
        sender.sendMessage(prefix + " §aПлагин \"" + pluginName + "\" включён.");
        return true;
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + " §cУкажите название плагина. Пример: /plreload disable Essentials");
            return true;
        }

        String pluginName = args[1];
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        if (plugin == null) {
            sender.sendMessage(prefix + " §cПлагин \"" + pluginName + "\" не найден.");
            return true;
        }

        if (!plugin.isEnabled()) {
            sender.sendMessage(prefix + " §eПлагин \"" + pluginName + "\" уже выключен.");
            return true;
        }

        if (plugin == this) {
            sender.sendMessage(prefix + " §cНельзя выключить PlReloader через самого себя.");
            return true;
        }

        Bukkit.getPluginManager().disablePlugin(plugin);
        sender.sendMessage(prefix + " §aПлагин \"" + pluginName + "\" выключен.");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        Arrays.sort(plugins, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(" §fСписок плагинов (§e").append(plugins.length).append("§f):\n");

        for (Plugin plugin : plugins) {
            String status = plugin.isEnabled() ? "§a✔" : "§c✘";
            sb.append(" ").append(status).append(" §f").append(plugin.getName());
            sb.append(" §7v").append(plugin.getDescription().getVersion());
            if (plugin == this) {
                sb.append(" §7(текущий)");
            }
            sb.append("\n");
        }

        sender.sendMessage(sb.toString().trim());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("plreloader.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "enable", "disable", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                    .map(Plugin::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
