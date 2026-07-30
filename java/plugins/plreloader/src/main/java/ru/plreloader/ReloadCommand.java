package ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final PlReloader plugin = PlReloader.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "plreload":
                return reloadPlugin(sender, args);
            case "plreloadall":
                return reloadAll(sender);
            case "plenable":
                return enablePlugin(sender, args);
            case "pldisable":
                return disablePlugin(sender, args);
            case "pllist":
                return listPlugins(sender);
        }
        return false;
    }

    private boolean reloadPlugin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plreloader.reload")) {
            sender.sendMessage(plugin.getPrefix() + "&cУ вас нет прав на перезагрузку плагинов.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix() + "&cИспользование: /plreload <плагин>");
            return true;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "&cПлагин \"" + args[0] + "\" не найден.");
            return true;
        }

        String name = target.getName();
        PluginManager pm = Bukkit.getPluginManager();

        if (name.equals("PlReloader")) {
            sender.sendMessage(plugin.getPrefix() + "&cНельзя перезагрузить PlReloader через самого себя.");
            return true;
        }

        pm.disablePlugin(target);
        pm.enablePlugin(target);

        sender.sendMessage(plugin.getPrefix() + "&aПлагин \"" + name + "\" успешно перезагружен!");
        return true;
    }

    private boolean reloadAll(CommandSender sender) {
        if (!sender.hasPermission("plreloader.reload")) {
            sender.sendMessage(plugin.getPrefix() + "&cУ вас нет прав на перезагрузку плагинов.");
            return true;
        }

        PluginManager pm = Bukkit.getPluginManager();
        int count = 0;

        for (Plugin p : pm.getPlugins()) {
            if (p.isEnabled() && !p.getName().equals("PlReloader")) {
                pm.disablePlugin(p);
                pm.enablePlugin(p);
                count++;
            }
        }

        sender.sendMessage(plugin.getPrefix() + "&aПерезагружено плагинов: " + count);
        return true;
    }

    private boolean enablePlugin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plreloader.enable")) {
            sender.sendMessage(plugin.getPrefix() + "&cУ вас нет прав на включение плагинов.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix() + "&cИспользование: /plenable <плагин>");
            return true;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "&cПлагин \"" + args[0] + "\" не найден.");
            return true;
        }

        if (target.isEnabled()) {
            sender.sendMessage(plugin.getPrefix() + "&eПлагин \"" + target.getName() + "\" уже включён.");
            return true;
        }

        Bukkit.getPluginManager().enablePlugin(target);
        sender.sendMessage(plugin.getPrefix() + "&aПлагин \"" + target.getName() + "\" включён!");
        return true;
    }

    private boolean disablePlugin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plreloader.disable")) {
            sender.sendMessage(plugin.getPrefix() + "&cУ вас нет прав на отключение плагинов.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.getPrefix() + "&cИспользование: /pldisable <плагин>");
            return true;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getPrefix() + "&cПлагин \"" + args[0] + "\" не найден.");
            return true;
        }

        if (target.getName().equals("PlReloader")) {
            sender.sendMessage(plugin.getPrefix() + "&cНельзя отключить PlReloader через самого себя.");
            return true;
        }

        if (!target.isEnabled()) {
            sender.sendMessage(plugin.getPrefix() + "&eПлагин \"" + target.getName() + "\" уже выключен.");
            return true;
        }

        Bukkit.getPluginManager().disablePlugin(target);
        sender.sendMessage(plugin.getPrefix() + "&aПлагин \"" + target.getName() + "\" выключен!");
        return true;
    }

    private boolean listPlugins(CommandSender sender) {
        if (!sender.hasPermission("plreloader.list")) {
            sender.sendMessage(plugin.getPrefix() + "&cУ вас нет прав на просмотр списка плагинов.");
            return true;
        }

        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        StringBuilder sb = new StringBuilder();
        sb.append(plugin.getPrefix()).append("&fПлагины (&7").append(plugins.length).append("&f):\n");

        for (Plugin p : plugins) {
            String status = p.isEnabled() ? "&a✔" : "&c✘";
            sb.append(" ").append(status).append(" &f").append(p.getName())
              .append(" &7v").append(p.getDescription().getVersion()).append("\n");
        }

        sender.sendMessage(PlReloader.color(sb.toString().trim()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 0) return null;

        String lower = args[args.length - 1].toLowerCase();

        return Bukkit.getPluginManager().getPlugins().stream()
                .map(Plugin::getName)
                .filter(n -> n.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
