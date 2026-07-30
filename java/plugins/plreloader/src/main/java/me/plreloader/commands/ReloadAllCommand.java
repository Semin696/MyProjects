package me.plreloader.commands;

import me.plreloader.PLReloader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class ReloadAllCommand implements CommandExecutor {

    private final PLReloader plugin = PLReloader.getInstance();
    private final PluginManager pm = plugin.getServer().getPluginManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!plugin.hasPermission(sender, "plreloader.reloadall")) return true;

        Plugin[] plugins = pm.getPlugins();

        for (Plugin p : plugins) {
            if (p.equals(plugin)) continue;
            if (!p.isEnabled()) {
                pm.enablePlugin(p);
            }
            pm.disablePlugin(p);
            pm.enablePlugin(p);
        }

        sender.sendMessage(plugin.getMessage("reload-all-success"));
        return true;
    }
}
