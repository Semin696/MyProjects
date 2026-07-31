package org.nig.smp.bc.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.nig.smp.bc.BcPlugin;

public class BcMediaCommand extends Command {

    private final BcPlugin plugin;

    public BcMediaCommand(BcPlugin plugin) {
        super("bcmedia");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextComponent.fromLegacyText(plugin.msg("usage-media")));
            return;
        }

        if (!sender.hasPermission("*")) {
            sender.sendMessage(TextComponent.fromLegacyText(plugin.msg("no-permission")));
            return;
        }

        String message = String.join(" ", args);
        String line = plugin.msg("format-media", message);
        ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(line));
    }
}
