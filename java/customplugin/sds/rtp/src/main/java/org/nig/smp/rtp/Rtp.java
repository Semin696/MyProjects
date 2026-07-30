package org.nig.smp.rtp;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.rtp.command.RtpCommand;
import org.nig.smp.rtp.config.ConfigManager;

public final class Rtp extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        var command = getCommand("rtp");
        if (command != null) {
            command.setExecutor(new RtpCommand(this, configManager));
        }

        getLogger().info("RTP enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("RTP disabled");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
