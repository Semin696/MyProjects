package org.nig.smp.team;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final TeamManager manager;

    public TeamCommand(TeamManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игрок может использовать эту команду.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String result;

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-create"));
                    return true;
                }
                result = manager.createTeam(player, args[1]);
            }
            case "disband" -> result = manager.disbandTeam(player);
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-invite"));
                    return true;
                }
                result = manager.invitePlayer(player, args[1]);
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-join"));
                    return true;
                }
                result = manager.joinTeam(player, args[1]);
            }
            case "leave" -> result = manager.leaveTeam(player);
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-kick"));
                    return true;
                }
                result = manager.kickPlayer(player, args[1]);
            }
            case "sethome" -> result = manager.setHome(player);
            case "home" -> result = manager.home(player);
            case "info" -> {
                String teamName = args.length >= 2 ? args[1] : null;
                result = manager.info(player, teamName);
            }
            case "list" -> result = manager.list();
            default -> {
                sendHelp(player);
                return true;
            }
        }

        player.sendMessage(result);
        return true;
    }

    private void sendHelp(Player player) {
        String p = manager.getPf();
        player.sendMessage(p + "§6=== /team ===");
        player.sendMessage("§e/team create <name> §7- создать команду");
        player.sendMessage("§e/team disband §7- распустить команду");
        player.sendMessage("§e/team invite <player> §7- пригласить игрока");
        player.sendMessage("§e/team join <name> §7- присоединиться");
        player.sendMessage("§e/team leave §7- покинуть команду");
        player.sendMessage("§e/team kick <player> §7- кикнуть игрока");
        player.sendMessage("§e/team sethome §7- установить дом");
        player.sendMessage("§e/team home §7- телепортироваться домой");
        player.sendMessage("§e/team info [name] §7- информация о команде");
        player.sendMessage("§e/team list §7- список команд");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            return List.of("create", "disband", "invite", "join", "leave",
                "kick", "sethome", "home", "info", "list").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
