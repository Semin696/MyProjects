package org.nig.smp.bc.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.bc.BcPlugin;

public class BcMediaCommand implements CommandExecutor {

    private final BcPlugin plugin;

    public BcMediaCommand(BcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage-media"));
            return true;
        }

        if (!sender.hasPermission("*")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        String message = String.join(" ", args);
        String line = plugin.msg("format-media", "message", message);
        plugin.getServer().broadcastMessage(line);
        return true;
    }
}
