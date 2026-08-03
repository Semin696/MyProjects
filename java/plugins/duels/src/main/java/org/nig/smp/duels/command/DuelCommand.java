package org.nig.smp.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.manager.DuelManager;

import java.util.ArrayList;
import java.util.List;

public final class DuelCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("cancel", "reload");

    private final DuelsPlugin plugin;
    private final DuelManager duelManager;

    public DuelCommand(DuelsPlugin plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("duel-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Только игроки могут использовать эту команду");
                return true;
            }
            duelManager.cancel(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("duels.admin")) {
                sender.sendMessage(plugin.msg("player-not-found"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getArenaManager().load();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        if (!(sender instanceof Player challenger)) {
            sender.sendMessage("Только игроки могут использовать эту команду");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            challenger.sendMessage(plugin.msg("player-not-found"));
            return true;
        }

        duelManager.createDirectChallenge(challenger, target);
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
