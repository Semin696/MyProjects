package org.nig.smp.settings;

import org.bukkit.plugin.java.JavaPlugin;

public final class Settings extends JavaPlugin {

    private MessageConfig messageConfig;
    private SettingsManager settingsManager;
    private SettingsGUI settingsGUI;
    private SettingsListener settingsListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageConfig = new MessageConfig(this);
        settingsManager = new SettingsManager(this);
        settingsGUI = new SettingsGUI(settingsManager, messageConfig);
        settingsListener = new SettingsListener(settingsManager, messageConfig);

        getCommand("settings").setExecutor(new SettingsCommand(settingsGUI, messageConfig));
        getServer().getPluginManager().registerEvents(settingsGUI, this);
        getServer().getPluginManager().registerEvents(settingsListener, this);

        getLogger().info("Settings plugin enabled");
    }

    @Override
    public void onDisable() {
        if (settingsManager != null) {
            settingsManager.save();
        }
        getLogger().info("Settings plugin disabled");
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}
