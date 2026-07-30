package org.nig.smp.groups;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.groups.commands.AdminCommand;
import org.nig.smp.groups.commands.DonateCommand;
import org.nig.smp.groups.config.ConfigManager;
import org.nig.smp.groups.expansion.DonationExpansion;

public final class Groups extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.reload();

        getCommand("donate").setExecutor(new DonateCommand(this));
        getCommand("donations").setExecutor(new AdminCommand(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DonationExpansion(this).register();
            getLogger().info("§a✔ PlaceholderAPI expansion registered!");
        } else {
            getLogger().warning("§ePlaceholderAPI not found — placeholders disabled");
        }

        getLogger().info("§a✔ Donations plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c✘ Donations plugin disabled!");
    }

    public ConfigManager getCfg() {
        return configManager;
    }
}
