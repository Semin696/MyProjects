package org.nig.smp.duels.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nig.smp.duels.DuelsPlugin;

public final class VisibilityManager {

    private final DuelsPlugin plugin;

    public VisibilityManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean viewerInDuels = plugin.getDuelsWorld() != null
                && viewer.getWorld().getName().equals(plugin.getDuelsWorld().getName());
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer == target) {
                    continue;
                }
                boolean targetInDuels = plugin.getDuelsWorld() != null
                    && target.getWorld().getName().equals(plugin.getDuelsWorld().getName());
                if (!viewerInDuels || !targetInDuels) {
                    viewer.showPlayer(plugin, target);
                } else {
                    if (plugin.getDuelManager().sameActiveDuel(viewer, target)) {
                        viewer.showPlayer(plugin, target);
                    } else {
                        viewer.hidePlayer(plugin, target);
                    }
                }
            }
        }
    }
}
