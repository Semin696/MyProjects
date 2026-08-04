package org.nig.smp.connectapi;

import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.connectapi.command.ConnectApiCommand;
import org.nig.smp.connectapi.core.AccountManager;
import org.nig.smp.connectapi.core.ConsoleForwarder;
import org.nig.smp.connectapi.core.MessageHandler;
import org.nig.smp.connectapi.core.SessionManager;
import org.nig.smp.connectapi.core.StatusManager;
import org.nig.smp.connectapi.websocket.WebSocketServer;

import java.util.List;

public final class ConnectApiPlugin extends JavaPlugin {

    private WebSocketServer wsServer;
    private ConsoleForwarder consoleForwarder;
    private AccountManager accounts;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String host = getConfig().getString("host", "0.0.0.0");
        int port = getConfig().getInt("port", 38710);

        this.accounts = new AccountManager(this);

        SessionManager sessions = new SessionManager();
        StatusManager statusManager = new StatusManager(this, sessions);
        MessageHandler handler = new MessageHandler(this, sessions, statusManager, accounts);

        this.wsServer = new WebSocketServer(host, port, sessions, handler);
        this.consoleForwarder = ConsoleForwarder.start(sessions);

        getCommand("connectapi").setExecutor(new ConnectApiCommand(accounts));

        wsServer.start();
        statusManager.start();

        getLogger().info("ConnectApi enabled on ws://" + host + ":" + port
                + " (accounts: " + accounts.getAccounts().size()
                + ", allowed commands: " + getAllowedCommands().size() + ")");
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

    public AccountManager getAccounts() {
        return accounts;
    }

    public boolean forwardChat() {
        return getConfig().getBoolean("forward-chat", true);
    }

    public List<String> getAllowedCommands() {
        return getConfig().getStringList("allowed-commands");
    }
}
