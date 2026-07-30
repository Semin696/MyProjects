package me.plreloader.commands;

import me.plreloader.PLReloader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class DisableCommand implements CommandExecutor {

    private final PLReloader plugin = PLReloader.getInstance();
    private final PluginManager pm = plugin.getServer().getPluginManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!plugin.hasPermission(sender, "plreloader.disable")) return true;

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage-disable"));
            return true;
        }

        String pluginName = args[0];
        Plugin target = pm.getPlugin(pluginName);

        if (target == null) {
            sender.sendMessage(plugin.getMessage("plugin-not-found", "plugin", pluginName));
            return true;
        }

        if (!target.isEnabled()) {
            sender.sendMessage(plugin.getMessage("plugin-already-disabled", "plugin", pluginName));
            return true;
        }

        pm.disablePlugin(target);
        sender.sendMessage(plugin.getMessage("plugin-disabled", "plugin", pluginName));
        return true;
    }
}
