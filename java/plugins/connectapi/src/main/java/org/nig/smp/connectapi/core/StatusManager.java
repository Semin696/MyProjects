package org.nig.smp.connectapi.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.nig.smp.connectapi.ConnectApiPlugin;
import org.nig.smp.connectapi.websocket.WebSocketConnection;

import java.util.List;

public class StatusManager implements Listener {

    private final ConnectApiPlugin plugin;
    private final SessionManager sessions;
    private final long startedAt = System.currentTimeMillis();

    public StatusManager(ConnectApiPlugin plugin, SessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (sessions.countAuthed() > 0) {
                sessions.broadcast(statusMessage());
            }
        }, 20L * 5, 20L * 5);
    }

    public void sendStatus(WebSocketConnection conn) {
        conn.send(statusMessage().toString());
    }

    private JsonObject statusMessage() {
        org.bukkit.Server server = plugin.getServer();
        JsonObject obj = new JsonObject();
        obj.addProperty("op", "status");
        obj.addProperty("online", server.getOnlinePlayers().size());
        obj.addProperty("max", server.getMaxPlayers());
        obj.addProperty("motd", server.getMotd());
        obj.addProperty("version", server.getVersion());
        obj.addProperty("bukkitVersion", server.getBukkitVersion());
        obj.addProperty("uptime", (System.currentTimeMillis() - startedAt) / 1000);
        try {
            double[] tps = server.getTPS();
            obj.addProperty("tps", tps.length > 0 ? tps[0] : 20.0);
        } catch (Throwable ignored) {
            obj.addProperty("tps", 20.0);
        }
        com.google.gson.JsonArray players = new com.google.gson.JsonArray();
        for (org.bukkit.entity.Player p : server.getOnlinePlayers()) {
            players.add(p.getName());
        }
        obj.add("players", players);
        com.google.gson.JsonArray worlds = new com.google.gson.JsonArray();
        for (org.bukkit.World w : server.getWorlds()) {
            worlds.add(w.getName());
        }
        obj.add("worlds", worlds);
        return obj;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.forwardChat()) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("op", "chat");
        obj.addProperty("player", event.getPlayer().getName());
        obj.addProperty("message", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
        sessions.broadcast(obj);
    }
}
