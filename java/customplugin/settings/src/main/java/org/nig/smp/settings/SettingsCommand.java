package org.nig.smp.settings;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SettingsCommand implements CommandExecutor {

    private final SettingsGUI gui;
    private final MessageConfig msg;

    public SettingsCommand(SettingsGUI gui, MessageConfig msg) {
        this.gui = gui;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.prefix() + " " + msg.commandOnlyPlayers());
            return true;
        }
        gui.open(player);
        return true;
    }
}
