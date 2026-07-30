package me.plreloader.commands;

import me.plreloader.PLReloader;
import me.plreloader.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ListCommand implements CommandExecutor {

    private final PLReloader plugin = PLReloader.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!plugin.hasPermission(sender, "plreloader.list")) return true;

        Plugin[] plugins = plugin.getServer().getPluginManager().getPlugins();
        String prefix = plugin.getPref();

        String headerPath = "messages.plugin-list-header";
        String header = plugin.getConfig().getString(headerPath, "&6Список плагинов (&f%count%&6):");
        header = header.replace("%count%", String.valueOf(plugins.length));
        sender.sendMessage(MessageUtil.colorize(prefix + header));

        String entryTemplate = plugin.getConfig().getString("messages.plugin-list-entry", " &7- %status% &f%name% &7(%version%)");

        for (Plugin p : plugins) {
            String statusPath = p.isEnabled() ? "status-enabled" : "status-disabled";
            String status = plugin.getConfig().getString("messages." + statusPath, "?");
            String entry = entryTemplate
                    .replace("%status%", status)
                    .replace("%name%", p.getName())
                    .replace("%version%", p.getDescription().getVersion());
            sender.sendMessage(MessageUtil.colorize(prefix + entry));
        }

        return true;
    }
}
