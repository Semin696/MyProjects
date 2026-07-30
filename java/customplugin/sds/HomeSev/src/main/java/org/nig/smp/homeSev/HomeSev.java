package org.nig.smp.homeSev;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.homeSev.command.DelHomeCommand;
import org.nig.smp.homeSev.command.HomeCommand;
import org.nig.smp.homeSev.command.SetHomeCommand;
import org.nig.smp.homeSev.manager.HomeManager;

public final class HomeSev extends JavaPlugin {

    private HomeManager homeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.homeManager = new HomeManager(this);

        var homeCmd = getCommand("home");
        var setHomeCmd = getCommand("sethome");
        var delHomeCmd = getCommand("delhome");

        if (homeCmd != null) {
            var executor = new HomeCommand(this, homeManager);
            homeCmd.setExecutor(executor);
            homeCmd.setTabCompleter(executor);
        }
        if (setHomeCmd != null) {
            setHomeCmd.setExecutor(new SetHomeCommand(this, homeManager));
        }
        if (delHomeCmd != null) {
            var executor = new DelHomeCommand(this, homeManager);
            delHomeCmd.setExecutor(executor);
            delHomeCmd.setTabCompleter(executor);
        }

        getLogger().info("HomeSev enabled!");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) {
            homeManager.saveHomes();
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }
}
