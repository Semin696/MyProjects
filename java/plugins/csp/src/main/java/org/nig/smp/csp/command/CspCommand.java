package org.nig.smp.csp.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nig.smp.csp.manager.BanManager;

import java.util.List;

public class CspCommand implements TabExecutor {

    private final BanManager banManager;

    public CspCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("unban")) {
            sender.sendMessage(Component.text("Usage: /csp unban <ip|nick>", NamedTextColor.RED));
            return true;
        }

        String target = args[1];

        if (banManager.getBan(target) == null) {
            sender.sendMessage(Component.text("No ban found for: " + target, NamedTextColor.YELLOW));
            return true;
        }

        banManager.removeBan(target);
        sender.sendMessage(Component.text("Unbanned: " + target, NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("unban");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("unban")) {
            return banManager.getAllBans().stream()
                .map(b -> b.getIp() + " (" + b.getNick() + ")")
                .toList();
        }
        return List.of();
    }
}
