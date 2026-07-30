package org.nig.smp.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.nig.smp.tpa.command.RespondCommand;
import org.nig.smp.tpa.command.TpaCommand;
import org.nig.smp.tpa.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Tpa extends JavaPlugin {

    private TpaManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new TpaManager();

        getCommand("tpa").setExecutor(new TpaCommand(manager, this));
        getCommand("tpahere").setExecutor(new TpaCommand(manager, this));
        getCommand("tpaccept").setExecutor(new RespondCommand(manager, this));
        getCommand("tpadeny").setExecutor(new RespondCommand(manager, this));

        getServer().getPluginManager().registerEvents(new PlayerListener(manager), this);

        getLogger().info("TPA плагин включён");
    }

    @Override
    public void onDisable() {
        getLogger().info("TPA плагин выключён");
    }

    public Component getPrefix() {
        String raw = getConfig().getString("prefix", "&8[&c&lmcru&8]");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw + " ");
    }

    public Component getMessage(String path, String... placeholders) {
        String text = getConfig().getString("messages." + path);
        if (text == null) {
            return Component.text("Сообщение не найдено: " + path);
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
