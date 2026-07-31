package org.nig.smp.bc.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.bc.BcPlugin;

public class BcCommand implements CommandExecutor {

    private final BcPlugin plugin;

    public BcCommand(BcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage"));
            return true;
        }

        String message = String.join(" ", args);

        if (sender.hasPermission("*")) {
            String line = plugin.msg("format-anonymous", "message", message);
            plugin.getServer().broadcastMessage(line);
            return true;
        }

        if (!sender.hasPermission("mediabc")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        String line = plugin.msg("format-named", "message", message, "name", sender.getName());
        plugin.getServer().broadcastMessage(line);
        return true;
    }
}
