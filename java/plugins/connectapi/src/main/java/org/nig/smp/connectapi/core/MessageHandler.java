package org.nig.smp.connectapi.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.nig.smp.connectapi.ConnectApiPlugin;
import org.nig.smp.connectapi.websocket.WebSocketConnection;

import java.util.List;

public class MessageHandler {

    private static final Gson GSON = new Gson();

    private final ConnectApiPlugin plugin;
    private final SessionManager sessions;
    private final StatusManager status;

    public MessageHandler(ConnectApiPlugin plugin, SessionManager sessions, StatusManager status) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.status = status;
    }

    public void onOpen(WebSocketConnection conn) {
        JsonObject hello = new JsonObject();
        hello.addProperty("op", "hello");
        hello.addProperty("name", "ConnectApi");
        hello.addProperty("authRequired", !plugin.getPassword().isEmpty());
        conn.send(hello.toString());
    }

    public void onClose(WebSocketConnection conn, String reason) {
        // nothing to do
    }

    public void onText(WebSocketConnection conn, String text) {
        JsonObject msg;
        try {
            JsonElement el = GSON.fromJson(text, JsonElement.class);
            if (el == null || !el.isJsonObject()) {
                sendError(conn, "expected a json object");
                return;
            }
            msg = el.getAsJsonObject();
        } catch (Exception e) {
            sendError(conn, "invalid json");
            return;
        }

        String op = str(msg, "op");
        if (op == null) {
            sendError(conn, "missing op");
            return;
        }

        switch (op) {
            case "auth": {
                String password = str(msg, "password");
                if (password == null) password = str(msg, "token"); // обратная совместимость
                boolean ok = password != null && !plugin.getPassword().isEmpty() && password.equals(plugin.getPassword());
                JsonObject r = new JsonObject();
                r.addProperty("op", "auth");
                r.addProperty("ok", ok);
                if (ok) {
                    sessions.markAuthed(conn);
                    r.addProperty("message", "authenticated");
                    conn.send(r.toString());
                    status.sendStatus(conn);
                } else {
                    r.addProperty("message", "invalid password");
                    conn.send(r.toString());
                    conn.close("bad password");
                }
                break;
            }
            case "status": {
                if (!requireAuthed(conn)) return;
                status.sendStatus(conn);
                break;
            }
            case "command": {
                if (!requireAuthed(conn)) return;
                String cmd = str(msg, "cmd");
                if (cmd == null || cmd.trim().isEmpty()) {
                    sendError(conn, "empty command");
                    return;
                }
                String finalCmd = cmd.trim();
                if (!allowed(finalCmd)) {
                    sendError(conn, "command not allowed: " + firstWord(finalCmd));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean ok = false;
                    try {
                        ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to run web command: " + finalCmd + " (" + t + ")");
                    }
                    JsonObject r = new JsonObject();
                    r.addProperty("op", "cmd_result");
                    r.addProperty("cmd", finalCmd);
                    r.addProperty("ok", ok);
                    conn.send(r.toString());
                });
                break;
            }
            case "broadcast": {
                if (!requireAuthed(conn)) return;
                String message = str(msg, "message");
                if (message == null || message.trim().isEmpty()) {
                    sendError(conn, "empty message");
                    return;
                }
                String finalMessage = message.trim();
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.broadcastMessage("\u00a76[\u00a7fWEB\u00a76] \u00a7f" + finalMessage));
                break;
            }
            case "ping": {
                JsonObject r = new JsonObject();
                r.addProperty("op", "pong");
                conn.send(r.toString());
                break;
            }
            default:
                sendError(conn, "unknown op: " + op);
        }
    }

    private boolean requireAuthed(WebSocketConnection conn) {
        if (!conn.isAuthed()) {
            sendError(conn, "not authenticated");
            return false;
        }
        return true;
    }

    private boolean allowed(String cmd) {
        List<String> allowed = plugin.getAllowedCommands();
        if (allowed.isEmpty()) return true;
        String first = firstWord(cmd).toLowerCase();
        for (String a : allowed) {
            if (a != null && a.equalsIgnoreCase(first)) return true;
        }
        return false;
    }

    private static String firstWord(String cmd) {
        int idx = cmd.indexOf(' ');
        return idx < 0 ? cmd : cmd.substring(0, idx);
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private static void sendError(WebSocketConnection conn, String message) {
        JsonObject r = new JsonObject();
        r.addProperty("op", "error");
        r.addProperty("message", message);
        conn.send(r.toString());
    }
}
