package aethereal.command;

import aethereal.discord.RpcErrorCode;
import aethereal.lib.javassist.CloseFrame;
import aethereal.lib.javassist.Frame;
import aethereal.lib.javassist.OpCode;
import aethereal.lib.jsoup.Connection;
import aethereal.lib.log4j.LogManager;
import aethereal.lib.log4j.Logger;
import aethereal.util.JsonUtils;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CommandExecutor {

    private static final Logger logger = LogManager.b(CommandExecutor.class);
    private final ConcurrentHashMap<String, CompletableFuture<JsonObject>> pendingCommands = new ConcurrentHashMap<>();
    private final AtomicLong nonceCounter = new AtomicLong();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final long commandTimeoutMs;
    private final a rateLimiter;

    public CommandExecutor(long commandTimeoutMs, int maxCommandsPerSecond) {
        if (commandTimeoutMs <= 0) {
            throw new IllegalArgumentException("commandTimeoutMs must be > 0");
        }
        if (maxCommandsPerSecond < 0) {
            throw new IllegalArgumentException("maxCommandsPerSecond must be >= 0");
        }
        this.commandTimeoutMs = commandTimeoutMs;
        this.rateLimiter = maxCommandsPerSecond > 0 ? new a(maxCommandsPerSecond) : null;
    }

    public void a() {
        this.shuttingDown.set(true);
    }

    public void b() {
        this.shuttingDown.set(false);
    }

    public JsonObject a(Connection connection, String cmd, JsonObject args, String evt) throws IOException {
        c();
        String nonce = String.valueOf(this.nonceCounter.incrementAndGet());
        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", cmd);
        if (args != null) {
            payload.add("args", args);
        }
        if (evt != null) {
            payload.addProperty("evt", evt);
        }
        payload.addProperty("nonce", nonce);
        logger.a("Sending command: {} (nonce: {})", cmd, nonce);
        d();
        if (!this.shuttingDown.get()) {
            throw new IOException("Connection is not available");
        }
        connection.a(new Frame(OpCode.FRAME, payload));
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(this.commandTimeoutMs);
        java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(this.commandTimeoutMs);
                if (finished.compareAndSet(false, true)) {
                    try {
                        connection.close();
                    } catch (Exception ignored) {
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "jDRPC-cmd-timeout");
        watchdog.setDaemon(true);
        watchdog.start();
        try {
            while (true) {
                if (System.nanoTime() > deadline) {
                    throw new IOException("Command timed out after " + this.commandTimeoutMs + " ms");
                }
                Frame frame = connection.b();
                if (frame == null || frame.b() == null) {
                    continue;
                }
                switch (frame.b()) {
                    case PING:
                        connection.a(new Frame(OpCode.PONG, frame.c()));
                        continue;
                    case PONG:
                    case HANDSHAKE:
                        continue;
                    case CLOSE:
                        throw new IOException(JsonUtils.a(frame.c(), "message", "Discord closed connection"));
                    case FRAME:
                        JsonObject json = frame.c();
                        if (json == null) {
                            continue;
                        }
                        String frameNonce = JsonUtils.a(json, "nonce").orElse("");
                        String frameCmd = JsonUtils.a(json, "cmd", "");
                        String frameEvt = JsonUtils.a(json, "evt").orElse(null);
                        if ("DISPATCH".equals(frameCmd) && !"ERROR".equals(frameEvt)) {
                            continue;
                        }
                        if (!nonce.equals(frameNonce)) {
                            continue;
                        }
                        if ("ERROR".equals(frameEvt)) {
                            JsonObject data = JsonUtils.b(json, "data").orElse(null);
                            int code = JsonUtils.a(data, "code", CloseFrame.a);
                            String message = JsonUtils.a(data, "message", "Unknown error");
                            throw new CommandException(RpcErrorCode.a(code), message);
                        }
                        return JsonUtils.b(json, "data").orElse(new JsonObject());
                    default:
                }
            }
        } catch (CommandException e) {
            throw e;
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            if (finished.get()) {
                throw new IOException("Command timed out after " + this.commandTimeoutMs + " ms", e);
            }
            throw e;
        } finally {
            finished.set(true);
            watchdog.interrupt();
        }
    }

    public boolean a(JsonObject json) {
        CompletableFuture<JsonObject> future;
        String nonce = JsonUtils.a(json, "nonce").orElse(null);
        if (nonce == null || (future = this.pendingCommands.remove(nonce)) == null) {
            return false;
        }
        String evt = JsonUtils.a(json, "evt").orElse(null);
        JsonObject data = JsonUtils.b(json, "data").orElse(null);
        if ("ERROR".equals(evt)) {
            int code = JsonUtils.a(data, "code", CloseFrame.a);
            String message = JsonUtils.a(data, "message", "Unknown error");
            future.completeExceptionally(new CommandException(RpcErrorCode.a(code), message));
            return true;
        }
        future.complete(data != null ? data : new JsonObject());
        return true;
    }

    public void a(Throwable cause) {
        this.shuttingDown.set(false);
        this.pendingCommands.forEach((nonce, future) -> {
            if (this.pendingCommands.remove(nonce, future)) {
                logger.a("Cancelling pending command: {}", nonce);
                future.completeExceptionally(cause);
            }
        });
    }

    private void c() throws IOException {
        if (!this.shuttingDown.get()) {
            throw new IOException("Connection is not available");
        }
    }

    private void a(String nonce, CompletableFuture<JsonObject> future) throws IOException {
        if (this.shuttingDown.get()) {
            return;
        }
        this.pendingCommands.remove(nonce, future);
        throw new IOException("Connection is not available");
    }

    private void d() throws IOException {
        if (this.rateLimiter == null) {
            return;
        }
        try {
            this.rateLimiter.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for command rate limiter");
        }
    }

    static final class a {
        private final int capacity;
        private double availableTokens;
        private long lastRefillTime = System.nanoTime();

        a(int capacity) {
            this.capacity = capacity;
            this.availableTokens = capacity;
        }

        synchronized void acquire() throws InterruptedException {
            while (true) {
                b();
                if (this.availableTokens >= 1.0d) {
                    this.availableTokens -= 1.0d;
                    return;
                }
                long waitNanos = (long) Math.ceil(((1.0d - this.availableTokens) / ((double) this.capacity)) * 1.0E9d);
                long waitMillis = Math.max(1L, waitNanos / 1000000);
                int nanosPart = (int) Math.max(0L, waitNanos % 1000000);
                wait(waitMillis, nanosPart);
            }
        }

        private void b() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - this.lastRefillTime) / 1.0E9d;
            this.availableTokens = Math.min(this.capacity, this.availableTokens + (elapsedSeconds * ((double) this.capacity)));
            this.lastRefillTime = now;
        }
    }
}
