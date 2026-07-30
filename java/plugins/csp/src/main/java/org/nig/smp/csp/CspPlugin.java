package org.nig.smp.csp;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.csp.command.CspCommand;
import org.nig.smp.csp.listener.PlayerLoginListener;
import org.nig.smp.csp.manager.BanManager;

public final class CspPlugin extends JavaPlugin {

    private BanManager banManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.banManager = new BanManager(getDataFolder());
        banManager.init();

        getServer().getPluginManager().registerEvents(new PlayerLoginListener(banManager), this);
        getCommand("csp").setExecutor(new CspCommand(banManager));

        getLogger().info("CSP enabled");
    }

    @Override
    public void onDisable() {
        if (banManager != null) {
            banManager.close();
        }
        getLogger().info("CSP disabled");
    }

    public BanManager getBanManager() {
        return banManager;
    }
}
