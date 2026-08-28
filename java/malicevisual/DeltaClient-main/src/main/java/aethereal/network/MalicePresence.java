package aethereal.network;

import aethereal.lib.log4j.LogManager;
import aethereal.lib.log4j.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class MalicePresence {
    private static final Logger LOGGER = LogManager.b(MalicePresence.class);
    private static final String TOPIC = "malice-visuals-presence-v1";
    private static final URI PUBLISH = URI.create("https://ntfy.sh/" + TOPIC);
    private static final URI POLL = URI.create("https://ntfy.sh/" + TOPIC + "/json?poll=1&since=2m");
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final AtomicBoolean busy = new AtomicBoolean();

    void sync(UUID self) {
        if (self == null || !this.busy.compareAndSet(false, true)) {
            return;
        }
        Util.getIoWorkerExecutor().execute(() -> {
            try {
                pull(self);
                push(self);
            } catch (Throwable error) {
                LOGGER.b("Malice presence failed: {}", rootMessage(error));
            } finally {
                this.busy.set(false);
            }
        });
    }

    private static void pull(UUID self) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(POLL)
                .timeout(TIMEOUT)
                .header("Accept", "application/x-ndjson, application/json")
                .header("User-Agent", "MaliceVisuals")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code == 404) {
            markOnClient(self);
            return;
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("poll " + code);
        }
        String body = response.body();
        if (body == null || body.isBlank()) {
            markOnClient(self);
            return;
        }
        for (String line : body.split("\n")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(line.trim());
            } catch (RuntimeException ignored) {
                continue;
            }
            if (!parsed.isJsonObject()) {
                continue;
            }
            JsonObject row = parsed.getAsJsonObject();
            String event = string(row, "event");
            if (event != null && !"message".equals(event)) {
                continue;
            }
            UUID uuid = parseCompact(string(row, "message"));
            if (uuid != null) {
                markOnClient(uuid);
            }
        }
        markOnClient(self);
    }

    private static void push(UUID self) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(PUBLISH)
                .timeout(TIMEOUT)
                .header("Content-Type", "text/plain; charset=utf-8")
                .header("User-Agent", "MaliceVisuals")
                .header("Title", "mv")
                .POST(HttpRequest.BodyPublishers.ofString(compact(self)))
                .build();
        int code = HTTP.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("publish " + code);
        }
    }

    private static void markOnClient(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> MaliceUsers.mark(uuid));
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static String compact(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private static UUID parseCompact(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String raw = value.trim();
        if (raw.length() == 32) {
            raw = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16)
                    + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
