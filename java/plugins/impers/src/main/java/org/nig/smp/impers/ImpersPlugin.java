package org.nig.smp.impers;

import org.bukkit.ChatColor;
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

        ImpersCommand command = new ImpersCommand(this, territoryManager, selections);
        getCommand("imp").setExecutor(command);
        getCommand("imp").setTabCompleter(command);
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

    public int getMaxSelectionSize() {
        return Math.max(1, getConfig().getInt("max-selection-size", 10));
    }

    public String msg(String key, Object... placeholders) {
        String msg = getConfig().getString("messages." + key, "");
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return msg;
    }
}
