package org.nig.smp.bc.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.nig.smp.bc.BcPlugin;

public class BcCommand extends Command {

    private final BcPlugin plugin;

    public BcCommand(BcPlugin plugin) {
        super("bc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextComponent.fromLegacyText(plugin.msg("usage")));
            return;
        }

        String message = String.join(" ", args);

        if (sender.hasPermission("*")) {
            String line = plugin.msg("format-anonymous", message);
            ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(line));
            return;
        }

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(TextComponent.fromLegacyText(plugin.msg("no-permission")));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        if (!player.hasPermission("mediabc")) {
            sender.sendMessage(TextComponent.fromLegacyText(plugin.msg("no-permission")));
            return;
        }

        String line = plugin.msg("format-named", message, player.getName());
        for (ProxiedPlayer p : player.getServer().getInfo().getPlayers()) {
            p.sendMessage(TextComponent.fromLegacyText(line));
        }
    }
}
