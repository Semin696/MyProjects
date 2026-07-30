package org.nig.smp.groups.expansion;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.groups.Groups;
import org.nig.smp.groups.config.ConfigManager.DonationLevel;

public class DonationExpansion extends PlaceholderExpansion {

    private final Groups plugin;

    public DonationExpansion(Groups plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "donations";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        // %donations_current% — текущий донат игрока (название с цветом)
        if (params.equalsIgnoreCase("current")) {
            DonationLevel level = plugin.getCfg().getCurrentLevel(player);
            return level != null ? level.name() : plugin.getCfg().getNoDonationText();
        }

        // %donations_current_id% — ID текущего доната (без цвета)
        if (params.equalsIgnoreCase("current_id")) {
            DonationLevel level = plugin.getCfg().getCurrentLevel(player);
            return level != null ? level.id() : "none";
        }

        // %donations_has_<id>% — true/false
        if (params.startsWith("has_")) {
            String id = params.substring(4);
            DonationLevel level = plugin.getCfg().getDonationLevels().get(id);
            if (level == null) return "false";
            if (level.permission().isEmpty()) return "true";
            return String.valueOf(player.hasPermission(level.permission()));
        }

        return null;
    }
}
