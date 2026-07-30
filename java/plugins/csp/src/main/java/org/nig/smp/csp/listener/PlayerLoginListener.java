package org.nig.smp.csp.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.nig.smp.csp.manager.BanManager;
import org.nig.smp.csp.model.BanEntry;



public class PlayerLoginListener implements Listener {

    private final BanManager banManager;

    public PlayerLoginListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();

        if (banManager.isBanned(ip)) {
            BanEntry ban = banManager.getBan(ip);
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Component.text()
                .append(Component.text("You are permanently banned from this server!", NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.GRAY))
                .append(Component.text(ban != null ? ban.getReason() : "Cheating", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("Banned by: ", NamedTextColor.GRAY))
                .append(Component.text(ban != null ? ban.getBannedBy() : "Console", NamedTextColor.WHITE))
                .build()
            );
        }
    }
}
