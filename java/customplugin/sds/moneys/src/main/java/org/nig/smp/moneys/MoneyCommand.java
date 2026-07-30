package org.nig.smp.moneys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final Moneys plugin;

    public MoneyCommand(Moneys plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Использование: /money give <игрок> <сумма>").color(NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("moneys.money.give")) {
            sender.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Игрок не найден!").color(NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Неверная сумма!").color(NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Сумма должна быть положительной!").color(NamedTextColor.RED));
            return true;
        }

        MoneyManager manager = plugin.getMoneyManager();
        manager.deposit(target.getUniqueId(), amount);

        sender.sendMessage(Component.text("Выдано " + manager.format(amount) + " игроку " + target.getName()).color(NamedTextColor.GREEN));

        Player online = target.getPlayer();
        if (online != null && online.isOnline()) {
            online.sendMessage(Component.text("Вы получили " + manager.format(amount)).color(NamedTextColor.GREEN));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase())) {
                return List.of("give");
            }
            return List.of();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("moneys.money.give")) {
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        return List.of();
    }
}
