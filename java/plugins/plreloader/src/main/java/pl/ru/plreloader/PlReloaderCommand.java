package pl.ru.plreloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PlReloaderCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "enable", "disable", "list", "reloadall", "help");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        PlReloader plugin = PlReloader.getInstance();
        String prefix = plugin.getPrefix();

        if (args.length == 0) {
            sender.sendMessage(prefix + ChatColor.YELLOW + "Используйте: /" + label + " help");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help": {
                sender.sendMessage(prefix + ChatColor.AQUA + "=== PlReloader — список команд ===");
                sender.sendMessage(ChatColor.GOLD + "/plreload reload <плагин>" + ChatColor.GRAY + " — Перезагрузить плагин");
                sender.sendMessage(ChatColor.GOLD + "/plreload enable <плагин>" + ChatColor.GRAY + " — Включить плагин");
                sender.sendMessage(ChatColor.GOLD + "/plreload disable <плагин>" + ChatColor.GRAY + " — Выключить плагин");
                sender.sendMessage(ChatColor.GOLD + "/plreload list" + ChatColor.GRAY + " — Список плагинов");
                sender.sendMessage(ChatColor.GOLD + "/plreload reloadall" + ChatColor.GRAY + " — Перезагрузить все плагины");
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("plreloader.reload") && !sender.hasPermission("plreloader.admin") && !sender.hasPermission("plreloader.use")) {
                    sender.sendMessage(prefix + ChatColor.RED + "У вас нет прав на эту команду.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + ChatColor.RED + "Укажите название плагина. Пример: /plreload reload Essentials");
                    return true;
                }
                String pluginName = args[1];
                Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
                if (target == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Плагин \"" + pluginName + "\" не найден.");
                    return true;
                }
                if (target.equals(plugin)) {
                    sender.sendMessage(prefix + ChatColor.RED + "Нельзя перезагрузить PlReloader через самого себя.");
                    return true;
                }
                plugin.reloadPlugin(pluginName);
                sender.sendMessage(prefix + ChatColor.GREEN + "Плагин \"" + pluginName + "\" успешно перезагружен!");
                return true;
            }
            case "enable": {
                if (!sender.hasPermission("plreloader.enable") && !sender.hasPermission("plreloader.admin") && !sender.hasPermission("plreloader.use")) {
                    sender.sendMessage(prefix + ChatColor.RED + "У вас нет прав на эту команду.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + ChatColor.RED + "Укажите название плагина. Пример: /plreload enable Essentials");
                    return true;
                }
                String pluginName = args[1];
                Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
                if (target == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Плагин \"" + pluginName + "\" не найден.");
                    return true;
                }
                if (target.isEnabled()) {
                    sender.sendMessage(prefix + ChatColor.YELLOW + "Плагин \"" + pluginName + "\" уже включён.");
                    return true;
                }
                plugin.enablePlugin(pluginName);
                sender.sendMessage(prefix + ChatColor.GREEN + "Плагин \"" + pluginName + "\" включён!");
                return true;
            }
            case "disable": {
                if (!sender.hasPermission("plreloader.disable") && !sender.hasPermission("plreloader.admin") && !sender.hasPermission("plreloader.use")) {
                    sender.sendMessage(prefix + ChatColor.RED + "У вас нет прав на эту команду.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + ChatColor.RED + "Укажите название плагина. Пример: /plreload disable Essentials");
                    return true;
                }
                String pluginName = args[1];
                Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
                if (target == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "Плагин \"" + pluginName + "\" не найден.");
                    return true;
                }
                if (target.equals(plugin)) {
                    sender.sendMessage(prefix + ChatColor.RED + "Нельзя выключить PlReloader через самого себя.");
                    return true;
                }
                if (!target.isEnabled()) {
                    sender.sendMessage(prefix + ChatColor.YELLOW + "Плагин \"" + pluginName + "\" уже выключен.");
                    return true;
                }
                plugin.disablePlugin(pluginName);
                sender.sendMessage(prefix + ChatColor.GREEN + "Плагин \"" + pluginName + "\" выключен!");
                return true;
            }
            case "list": {
                if (!sender.hasPermission("plreloader.list") && !sender.hasPermission("plreloader.admin") && !sender.hasPermission("plreloader.use")) {
                    sender.sendMessage(prefix + ChatColor.RED + "У вас нет прав на эту команду.");
                    return true;
                }
                Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                sender.sendMessage(prefix + ChatColor.AQUA + "=== Плагины (" + plugins.length + ") ===");
                for (Plugin p : plugins) {
                    String status = p.isEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
                    String marker = p.equals(plugin) ? ChatColor.GOLD + " [*]" : "";
                    sender.sendMessage(status + " " + ChatColor.WHITE + p.getName() + ChatColor.GRAY + " v" + p.getDescription().getVersion() + marker);
                }
                sender.sendMessage(ChatColor.GRAY + "✓ — включён  ✗ — выключен  [*] — PlReloader");
                return true;
            }
            case "reloadall": {
                if (!sender.hasPermission("plreloader.reloadall") && !sender.hasPermission("plreloader.admin") && !sender.hasPermission("plreloader.use")) {
                    sender.sendMessage(prefix + ChatColor.RED + "У вас нет прав на эту команду.");
                    return true;
                }
                Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
                int count = 0;
                for (Plugin p : plugins) {
                    if (p.equals(plugin)) continue;
                    if (!p.isEnabled()) continue;
                    plugin.reloadPlugin(p.getName());
                    count++;
                }
                sender.sendMessage(prefix + ChatColor.GREEN + "Перезагружено плагинов: " + count);
                return true;
            }
            default: {
                sender.sendMessage(prefix + ChatColor.RED + "Неизвестная подкоманда. Используйте /" + label + " help");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
            String partial = args[1].toLowerCase();
            return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
