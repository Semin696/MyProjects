package org.nig.smp.nightfall_motdicon;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.nightfall_motdicon.listener.MotdListener;
import org.nig.smp.nightfall_motdicon.manager.IconManager;
import org.nig.smp.nightfall_motdicon.manager.MotdManager;

public final class Nightfall_motdicon extends JavaPlugin {

    private MotdManager motdManager;
    private IconManager iconManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        motdManager = new MotdManager(this);
        iconManager = new IconManager(this);
        iconManager.loadIcon();
        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        getLogger().info("Enabled successfully");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("motdicon")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                motdManager.resetIndex();
                iconManager.loadIcon();
                sender.sendMessage("§aNightfall_motdicon config reloaded.");
                getLogger().info("Config reloaded by " + sender.getName());
            } else {
                sender.sendMessage("§6Usage: /motdicon reload");
            }
            return true;
        }
        return false;
    }

    public MotdManager getMotdManager() {
        return motdManager;
    }

    public IconManager getIconManager() {
        return iconManager;
    }
}
