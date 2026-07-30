package org.nig.smp.order;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class Config {

    private final Order plugin;
    private String prefix;
    private final Map<String, String> messages = new HashMap<>();

    public Config(Order plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();
        prefix = cfg.getString("prefix", "&8[&6Orders&8]");

        messages.clear();
        if (cfg.isConfigurationSection("messages")) {
            for (String key : cfg.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, cfg.getString("messages." + key, ""));
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public Component format(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, "&cMessage not found: " + key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + " " + msg);
    }

    public Component formatRaw(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, "&cMessage not found: " + key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
