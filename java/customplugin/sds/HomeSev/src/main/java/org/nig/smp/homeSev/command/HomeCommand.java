package org.nig.smp.homeSev.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.homeSev.HomeSev;
import org.nig.smp.homeSev.manager.HomeManager;
import org.nig.smp.homeSev.model.Home;

import java.util.List;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final HomeSev plugin;
    private final HomeManager homeManager;

    public HomeCommand(HomeSev plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            List<String> homeNames = homeManager.getHomeNames(player);
            if (homeNames.isEmpty()) {
                player.sendMessage(Component.text("You have no saved homes. Use /sethome <name> to set one.", NamedTextColor.YELLOW));
                return true;
            }
            if (homeNames.size() == 1) {
                String name = homeNames.getFirst();
                teleportToHome(player, name);
                return true;
            }
            player.sendMessage(Component.text("Your homes: " + String.join(", ", homeNames), NamedTextColor.GREEN));
            return true;
        }

        String homeName = args[0].toLowerCase();
        teleportToHome(player, homeName);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return homeManager.getHomeNames(player).stream()
                .filter(name -> name.startsWith(partial))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private void teleportToHome(Player player, String homeName) {
        Home home = homeManager.getHome(player, homeName);

        if (home == null) {
            player.sendMessage(Component.text("Home '" + homeName + "' not found!", NamedTextColor.RED));
            return;
        }

        var location = home.toLocation();
        if (location == null) {
            player.sendMessage(Component.text("The world for this home no longer exists!", NamedTextColor.RED));
            return;
        }

        player.teleportAsync(location);
        player.sendMessage(Component.text("Teleported to home '" + homeName + "'!", NamedTextColor.GREEN));
    }
}
