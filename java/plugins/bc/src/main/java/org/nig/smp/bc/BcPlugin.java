package org.nig.smp.bc;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.bc.command.BcCommand;
import org.nig.smp.bc.command.BcMediaCommand;

public final class BcPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        registerCommand("bc", new BcCommand(this));
        registerCommand("bcmedia", new BcMediaCommand(this));

        getLogger().info("BC enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("BC disabled");
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor((org.bukkit.command.CommandExecutor) executor);
        }
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
