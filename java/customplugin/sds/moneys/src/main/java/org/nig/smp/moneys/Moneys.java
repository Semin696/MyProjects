package org.nig.smp.moneys;

import org.bukkit.plugin.java.JavaPlugin;

public final class Moneys extends JavaPlugin {

    private MoneyManager moneyManager;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        moneyManager = new MoneyManager(this);

        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("money").setExecutor(new MoneyCommand(this));
        getServer().getPluginManager().registerEvents(new MobKillListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoneyPlaceholderExpansion(this).register();
        }

        getLogger().info("Moneys enabled!");
    }

    @Override
    public void onDisable() {
        if (moneyManager != null) {
            moneyManager.saveData();
        }
        getLogger().info("Moneys disabled!");
    }

    public MoneyManager getMoneyManager() {
        return moneyManager;
    }
}
