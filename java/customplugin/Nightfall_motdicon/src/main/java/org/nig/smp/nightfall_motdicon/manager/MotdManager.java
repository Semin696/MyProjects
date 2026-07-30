package org.nig.smp.nightfall_motdicon.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.server.ServerListPingEvent;
import org.nig.smp.nightfall_motdicon.Nightfall_motdicon;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MotdManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final Nightfall_motdicon plugin;
    private int currentIndex = 0;

    public MotdManager(Nightfall_motdicon plugin) {
        this.plugin = plugin;
    }

    public void applyMotd(ServerListPingEvent event) {
        List<String> lines = plugin.getConfig().getStringList("motd.lines");
        if (lines.isEmpty()) return;

        boolean cycle = plugin.getConfig().getBoolean("motd.cycle", true);
        String raw;

        if (cycle) {
            raw = lines.get(currentIndex % lines.size());
            currentIndex++;
        } else {
            raw = lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
        }

        event.motd(LEGACY.deserialize(raw.replace('&', '\u00A7')));
    }

    public void resetIndex() {
        currentIndex = 0;
    }
}
