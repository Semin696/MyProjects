package org.nig.smp.homeSev.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nig.smp.homeSev.HomeSev;
import org.nig.smp.homeSev.manager.HomeManager;
import org.nig.smp.homeSev.model.Home.Type;

public class SetHomeCommand implements CommandExecutor {

    private final HomeSev plugin;
    private final HomeManager homeManager;

    public SetHomeCommand(HomeSev plugin, HomeManager homeManager) {
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
            player.sendMessage(Component.text("Usage: /sethome <name>", NamedTextColor.RED));
            return true;
        }

        String homeName = args[0].toLowerCase();

        Type type;
        if (homeManager.isCommandHomeName(homeName)) {
            type = Type.COMMAND;
        } else {
            type = Type.STANDARD;
        }

        boolean isNew = homeManager.getHome(player, homeName) == null;

        if (isNew) {
            if (type == Type.STANDARD) {
                int current = homeManager.getStandardHomeCount(player);
                int limit = homeManager.getStandardLimit(player);
                if (current >= limit) {
                    player.sendMessage(Component.text("You have reached the limit of " + limit + " standard homes!", NamedTextColor.RED));
                    return true;
                }
            } else {
                int current = homeManager.getCommandHomeCount(player);
                int limit = homeManager.getCommandLimit(player);
                if (current >= limit) {
                    player.sendMessage(Component.text("You have reached the limit of " + limit + " command homes!", NamedTextColor.RED));
                    return true;
                }
            }
        }

        homeManager.setHome(player, homeName, player.getLocation(), type);
        player.sendMessage(Component.text("Home '" + homeName + "' has been set!", NamedTextColor.GREEN));

        return true;
    }
}
