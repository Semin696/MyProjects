package org.nig.smp.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.manager.DuelManager;
import org.nig.smp.duels.menu.KitSelectionMenu;

import java.util.ArrayList;
import java.util.List;

public final class DuelCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("queue", "lobby", "leave", "cancel", "reload");

    private final DuelsPlugin plugin;
    private final DuelManager duelManager;

    public DuelCommand(DuelsPlugin plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки могут использовать эту команду");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.msg("duel-usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "cancel":
                duelManager.cancel(player);
                return true;

            case "lobby":
                duelManager.enterDuels(player);
                return true;

            case "leave":
                duelManager.leaveDuels(player);
                return true;

            case "queue":
                if (duelManager.isBusy(player.getUniqueId())) {
                    player.sendMessage(plugin.msg("already-in-duel"));
                    return true;
                }
                if (plugin.getArenaManager().getArenas().isEmpty()) {
                    player.sendMessage(plugin.msg("no-arenas"));
                    return true;
                }
                new KitSelectionMenu(plugin, player).open();
                return true;

            case "reload":
                if (!player.hasPermission("duels.admin")) {
                    player.sendMessage(plugin.msg("player-not-found"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getArenaManager().load();
                player.sendMessage(plugin.msg("reloaded"));
                return true;

            default:
                break;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.msg("player-not-found"));
            return true;
        }

        duelManager.createDirectChallenge(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                result.add(online.getName());
            }
            result.addAll(SUBCOMMANDS);
            return filter(result, args[0]);
        }
        return List.of();
    }

    private List<String> filter(List<String> candidates, String prefix) {
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(candidate);
            }
        }
        return result;
    }
}
