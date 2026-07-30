package org.nig.smp.groups.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.groups.Groups;
import org.nig.smp.groups.menu.SettingsMenu;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final Groups plugin;

    public AdminCommand(Groups plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("donate.admin")) {
            sender.sendMessage(plugin.getCfg().msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            // /donations — open menu
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getCfg().msg("only-players"));
                return true;
            }
            SettingsMenu menu = new SettingsMenu(plugin);
            menu.open(player);
            player.sendMessage(plugin.getCfg().msg("menu-opened"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getCfg().reload();
            sender.sendMessage(plugin.getCfg().msg("config-reloaded"));
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("donate.admin")) {
            completions.add("reload");
        }
        return completions;
    }
}
