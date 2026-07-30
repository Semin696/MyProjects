package org.nig.smp.order;

import org.bukkit.plugin.java.JavaPlugin;

public final class Order extends JavaPlugin {

    private Config config;
    private PrivateChatManager chatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new Config(this);
        chatManager = new PrivateChatManager();

        getCommand("order").setExecutor(new OrderCommand(config, chatManager, this));
        getServer().getPluginManager().registerEvents(new ChatListener(chatManager, config), this);
    }

    @Override
    public void onDisable() {
    }

}
