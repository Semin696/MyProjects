package org.nig.smp.settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class MessageConfig {

    private final Settings plugin;

    public MessageConfig(Settings plugin) {
        this.plugin = plugin;
    }

    public String prefix() {
        return color(plugin.getConfig().getString("prefix", "&8[&6Settings&8]"));
    }

    public Component menuTitle() {
        return legacy(plugin.getConfig().getString("messages.menu_title", "&8\u2699 Настройки"));
    }

    public String enabled() {
        return color(plugin.getConfig().getString("messages.enabled", "&a\u2713 Включено"));
    }

    public String disabled() {
        return color(plugin.getConfig().getString("messages.disabled", "&c\u2717 Выключено"));
    }

    public String clickToToggle() {
        return color(plugin.getConfig().getString("messages.click_to_toggle", "&7Нажмите, чтобы переключить"));
    }

    public String commandOnlyPlayers() {
        return color(plugin.getConfig().getString("messages.command_only_players", "&cТолько для игроков"));
    }

    public String description(SettingType type) {
        return color(plugin.getConfig().getString("descriptions." + type.getKey(), type.getDescription()));
    }

    public static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
