package org.nig.smp.duels.manager;

import org.bukkit.ChatColor;
import org.nig.smp.duels.DuelsPlugin;

public final class KitManager {

    private final DuelsPlugin plugin;

    public KitManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public String getDisplayName(String realName) {
        String display = plugin.getConfig().getString("kits." + realName + ".display-name");
        if (display == null || display.isEmpty()) {
            return realName;
        }
        return ChatColor.translateAlternateColorCodes('&', display);
    }

    public boolean isDestructive(String realName) {
        return plugin.getConfig().getBoolean("kits." + realName + ".destructive", false);
    }
}
