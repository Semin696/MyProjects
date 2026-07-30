package com.github.plreloader.commands;

import com.github.plreloader.PluginReloader;
import com.github.plreloader.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final PluginReloader plugin;
    private final ConfigManager config;

    public ReloadCommand(PluginReloader plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(config.getPrefix() + "§cИспользование: /plreload <reload|enable|disable|list> [плагин]");
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
                sender.sendMessage(config.getPrefix() + "§cНеизвестная подкоманда. Используйте: reload, enable, disable, list");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + "§cУкажите название плагина для перезагрузки.");
            return true;
        }

        String pluginName = args[1];
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(config.getPrefix() + "§cПлагин §e" + pluginName + " §cне найден.");
            return true;
        }

        Bukkit.getPluginManager().disablePlugin(target);
        Bukkit.getPluginManager().enablePlugin(target);

        sender.sendMessage(config.getPrefix() + "§aПлагин §e" + target.getName() + " §aуспешно перезагружен!");
        return true;
    }

    private boolean handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + "§cУкажите название плагина для включения.");
            return true;
        }

        String pluginName = args[1];
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(config.getPrefix() + "§cПлагин §e" + pluginName + " §cне найден.");
            return true;
        }

        if (target.isEnabled()) {
            sender.sendMessage(config.getPrefix() + "§eПлагин " + target.getName() + " §eуже включён.");
            return true;
        }

        Bukkit.getPluginManager().enablePlugin(target);
        sender.sendMessage(config.getPrefix() + "§aПлагин §e" + target.getName() + " §aвключён!");
        return true;
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(config.getPrefix() + "§cУкажите название плагина для отключения.");
            return true;
        }

        String pluginName = args[1];
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(config.getPrefix() + "§cПлагин §e" + pluginName + " §cне найден.");
            return true;
        }

        if (!target.isEnabled()) {
            sender.sendMessage(config.getPrefix() + "§eПлагин " + target.getName() + " §eуже выключен.");
            return true;
        }

        if (target.equals(plugin)) {
            sender.sendMessage(config.getPrefix() + "§cНельзя отключить PLReloader через самого себя!");
            return true;
        }

        Bukkit.getPluginManager().disablePlugin(target);
        sender.sendMessage(config.getPrefix() + "§aПлагин §e" + target.getName() + " §aвыключен!");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        List<String> enabled = new ArrayList<>();
        List<String> disabled = new ArrayList<>();

        for (Plugin p : plugins) {
            if (p.isEnabled()) {
                enabled.add("§a" + p.getName());
            } else {
                disabled.add("§c" + p.getName());
            }
        }

        sender.sendMessage(config.getPrefix() + "§fСписок плагинов:");
        sender.sendMessage(" §a✓ Включены§f: " + String.join("§f, ", enabled));
        if (!disabled.isEmpty()) {
            sender.sendMessage(" §c✗ Отключены§f: " + String.join("§f, ", disabled));
        }
        sender.sendMessage(config.getPrefix() + "§7Всего: " + plugins.length + " | Включено: " + enabled.size() + " | Отключено: " + disabled.size());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "enable", "disable", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            String input = args[1].toLowerCase();
            return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                    .map(Plugin::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
