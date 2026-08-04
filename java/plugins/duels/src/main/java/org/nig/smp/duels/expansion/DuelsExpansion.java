package org.nig.smp.duels.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.nig.smp.duels.DuelsPlugin;
import org.nig.smp.duels.model.PlayerStats;

public final class DuelsExpansion extends PlaceholderExpansion {

    private final DuelsPlugin plugin;

    public DuelsExpansion(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "duels";
    }

    @Override
    public String getAuthor() {
        return "NightfallRealm";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        switch (params.toLowerCase()) {
            case "wins":
                return String.valueOf(stats.getWins());
            case "losses":
                return String.valueOf(stats.getLosses());
            case "total":
                return String.valueOf(stats.getTotal());
            case "winstreak":
                return String.valueOf(stats.getWinstreak());
            case "best_winstreak":
                return String.valueOf(stats.getBestWinstreak());
            default:
                return null;
        }
    }
}
