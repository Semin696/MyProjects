package org.nig.smp.connectapi.websocket;

import org.nig.smp.connectapi.core.MessageHandler;
import org.nig.smp.connectapi.core.SessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class WebSocketServer implements Runnable {

    private final String host;
    private final int port;
    private final SessionManager sessions;
    private final MessageHandler handler;

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private Thread thread;

    public WebSocketServer(String host, int port, SessionManager sessions, MessageHandler handler) {
        this.host = host;
        this.port = port;
        this.sessions = sessions;
        this.handler = handler;
    }

    public void start() {
        running = true;
        thread = new Thread(this, "ConnectApi-WebSocket");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        sessions.closeAll();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port));
        } catch (IOException e) {
            System.err.println("[ConnectApi] Failed to bind WebSocket server on " + host + ":" + port + ": " + e.getMessage());
            return;
        }
        System.out.println("[ConnectApi] WebSocket server listening on " + host + ":" + port);
        while (running) {
            try {
                Socket client = serverSocket.accept();
                Thread t = new Thread(new WebSocketConnection(client, sessions, handler), "ConnectApi-WsConn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("[ConnectApi] WebSocket accept error: " + e.getMessage());
                }
            }
        }
    }
}
