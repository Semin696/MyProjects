package org.nig.smp.pvp;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Pvp extends JavaPlugin {

    private CombatManager combatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        combatManager = new CombatManager(this);
        Bukkit.getPluginManager().registerEvents(combatManager, this);

        new CombatTimerTask().runTaskTimer(this, 0L, 20L);
    }

    @Override
    public void onDisable() {
        if (combatManager != null) {
            combatManager.cleanupAll();
        }
    }

    private class CombatTimerTask extends BukkitRunnable {
        @Override
        public void run() {
            combatManager.tick();
        }
    }
}
