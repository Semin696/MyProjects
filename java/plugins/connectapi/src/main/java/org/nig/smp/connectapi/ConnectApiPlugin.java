package org.nig.smp.connectapi;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.connectapi.core.ConsoleForwarder;
import org.nig.smp.connectapi.core.MessageHandler;
import org.nig.smp.connectapi.core.SessionManager;
import org.nig.smp.connectapi.core.StatusManager;
import org.nig.smp.connectapi.websocket.WebSocketServer;

import java.util.List;

public final class ConnectApiPlugin extends JavaPlugin {

    private WebSocketServer wsServer;
    private ConsoleForwarder consoleForwarder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String host = getConfig().getString("host", "0.0.0.0");
        int port = getConfig().getInt("port", 25570);

        SessionManager sessions = new SessionManager();
        StatusManager statusManager = new StatusManager(this, sessions);
        MessageHandler handler = new MessageHandler(this, sessions, statusManager);

        this.wsServer = new WebSocketServer(host, port, sessions, handler);
        this.consoleForwarder = ConsoleForwarder.start(sessions);

        wsServer.start();
        statusManager.start();

        boolean tokenChanged = !getPassword().isEmpty() && !"change-me".equals(getPassword());
        getLogger().info("ConnectApi enabled on ws://" + host + ":" + port
                + " (password set: " + tokenChanged + ", allowed commands: " + getAllowedCommands().size() + ")");
    }

    @Override
    public void onDisable() {
        if (consoleForwarder != null) {
            consoleForwarder.shutdown();
        }
        if (wsServer != null) {
            wsServer.stop();
        }
        getLogger().info("ConnectApi disabled");
    }

    public String getPassword() {
        return getConfig().getString("password", getConfig().getString("token", "change-me"));
    }

    public boolean forwardChat() {
        return getConfig().getBoolean("forward-chat", true);
    }

    public List<String> getAllowedCommands() {
        return getConfig().getStringList("allowed-commands");
    }
}
