package org.nig.smp.impers;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.impers.command.ImpersCommand;
import org.nig.smp.impers.listener.PlayerListener;
import org.nig.smp.impers.manager.TerritoryManager;
import org.nig.smp.impers.model.ChunkSelection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ImpersPlugin extends JavaPlugin {

    private TerritoryManager territoryManager;
    private PlayerListener playerListener;
    private final Map<UUID, ChunkSelection> selections = new HashMap<>();
    private int taskId;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.territoryManager = new TerritoryManager(this);
        territoryManager.load();

        this.playerListener = new PlayerListener(this, territoryManager, selections);

        getCommand("imp").setExecutor(new ImpersCommand(this, territoryManager, selections));
        getServer().getPluginManager().registerEvents(playerListener, this);

        taskId = getServer().getScheduler().runTaskTimer(this, playerListener::showSelectionParticles, 0L, 10L).getTaskId();

        getLogger().info("Impers enabled");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(taskId);
        if (territoryManager != null) {
            territoryManager.save();
        }
        getLogger().info("Impers disabled");
    }

    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }
}
