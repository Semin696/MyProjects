package org.nig.smp.bc;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.nig.smp.bc.command.BcCommand;
import org.nig.smp.bc.command.BcMediaCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class BcPlugin extends Plugin {

    private Configuration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        getProxy().getPluginManager().registerCommand(this, new BcCommand(this));
        getProxy().getPluginManager().registerCommand(this, new BcMediaCommand(this));

        getLogger().info("BC enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("BC disabled");
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadConfig() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String msg(String key, Object... placeholders) {
        String msg = config.getString("messages." + key, "");
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return msg;
    }
}
