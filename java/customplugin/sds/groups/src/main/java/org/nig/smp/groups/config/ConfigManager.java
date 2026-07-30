package org.nig.smp.groups.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.nig.smp.groups.Groups;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigManager {

    private final Groups plugin;
    private FileConfiguration config;

    private boolean enabled;
    private String noDonationText;
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Map<String, DonationLevel> donationLevels = new LinkedHashMap<>();

    public record DonationLevel(String id, String name, String permission) {}

    public ConfigManager(Groups plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        load();
    }

    private void load() {
        messages.clear();
        donationLevels.clear();

        enabled = config.getBoolean("settings.enabled", true);
        noDonationText = color(config.getString("settings.no-donation-text", "&7Нет"));

        ConfigurationSection msgSection = config.getConfigurationSection("settings.messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messages.put(key, color(msgSection.getString(key, "")));
            }
        }

        ConfigurationSection donSection = config.getConfigurationSection("donations");
        if (donSection != null) {
            for (String id : donSection.getKeys(false)) {
                ConfigurationSection sec = donSection.getConfigurationSection(id);
                if (sec == null) continue;
                String name = color(sec.getString("name", id));
                String permission = sec.getString("permission", "");
                donationLevels.put(id, new DonationLevel(id, name, permission));
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("settings.enabled", enabled);
        plugin.saveConfig();
    }

    public String getNoDonationText() {
        return noDonationText;
    }

    public String msg(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public String msg(String key, String... placeholders) {
        String msg = msg(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg;
    }

    public Map<String, DonationLevel> getDonationLevels() {
        return donationLevels;
    }

    public DonationLevel getCurrentLevel(org.bukkit.entity.Player player) {
        DonationLevel best = null;
        for (DonationLevel level : donationLevels.values()) {
            String perm = level.permission();
            if (perm.equals("*") || perm.isEmpty() || player.hasPermission(perm)) {
                best = level;
            }
        }
        return best;
    }

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
