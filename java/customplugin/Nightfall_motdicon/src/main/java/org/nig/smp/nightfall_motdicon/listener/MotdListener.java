package org.nig.smp.nightfall_motdicon.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.nig.smp.nightfall_motdicon.Nightfall_motdicon;

public class MotdListener implements Listener {

    private final Nightfall_motdicon plugin;

    public MotdListener(Nightfall_motdicon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        plugin.getMotdManager().applyMotd(event);
        plugin.getIconManager().applyIcon(event);
    }
}
