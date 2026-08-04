package org.nig.smp.connectapi.core;

import com.google.gson.JsonObject;
import org.nig.smp.connectapi.websocket.WebSocketConnection;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Set<WebSocketConnection> all = ConcurrentHashMap.newKeySet();
    private final Set<WebSocketConnection> authed = ConcurrentHashMap.newKeySet();

    public void add(WebSocketConnection conn) {
        all.add(conn);
    }

    public void remove(WebSocketConnection conn) {
        all.remove(conn);
        authed.remove(conn);
    }

    public void markAuthed(WebSocketConnection conn) {
        conn.setAuthed(true);
        authed.add(conn);
    }

    public int countAuthed() {
        return authed.size();
    }

    public void closeAll() {
        for (WebSocketConnection conn : all) {
            conn.close("plugin shutdown");
        }
    }

    public void broadcast(JsonObject msg) {
        String data = msg.toString();
        for (WebSocketConnection conn : authed) {
            conn.send(data);
        }
    }
}
