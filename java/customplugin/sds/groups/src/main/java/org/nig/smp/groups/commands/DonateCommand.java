package org.nig.smp.groups.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.groups.Groups;
import org.nig.smp.groups.config.ConfigManager.DonationLevel;

import java.util.ArrayList;
import java.util.List;

public class DonateCommand implements CommandExecutor, TabCompleter {

    private final Groups plugin;

    public DonateCommand(Groups plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        if (!plugin.getCfg().isEnabled()) {
            sender.sendMessage("§cСистема донатов временно отключена.");
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;

        if (player != null) {
            DonationLevel level = plugin.getCfg().getCurrentLevel(player);
            if (level != null) {
                sender.sendMessage("§a✔ Текущий донат: " + level.name());
            } else {
                sender.sendMessage(plugin.getCfg().getNoDonationText());
            }
        }

        if (sender.hasPermission("donate.admin")) {
            sender.sendMessage("§6╔══════════════════════════════╗");
            sender.sendMessage("§6║  §eАдминка донатов           §6║");
            sender.sendMessage("§6╠══════════════════════════════╣");
            sender.sendMessage("§6║ §f/donations §7— меню настроек");
            sender.sendMessage("§6║ §f/donations reload §7— перезагрузить конфиг");
            sender.sendMessage("§6╚══════════════════════════════╝");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
