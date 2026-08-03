package org.nig.smp.duels.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.cmi.CMIKitBridge;

import java.util.ArrayList;
import java.util.List;

public final class KitCommand implements CommandExecutor, TabCompleter {

    private final DuelsPlugin plugin;

    public KitCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("duels.admin")) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("kit-usage"));
            return true;
        }

        String kitName = args[0];
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }

        if (!CMIKitBridge.getKitNames().contains(kitName)) {
            sender.sendMessage(plugin.msg("kit-not-found", "kit", kitName));
            return true;
        }

        if (CMIKitBridge.applyKit(target, kitName)) {
            sender.sendMessage(plugin.msg("kit-given", "kit", kitName, "player", target.getName()));
        } else {
            sender.sendMessage(plugin.msg("kit-not-found", "kit", kitName));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(new ArrayList<>(CMIKitBridge.getKitNames()), args[0]);
        }
        if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                players.add(online.getName());
            }
            return filter(players, args[1]);
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
