package aethereal.discord;

import aethereal.config.BaseProcessor;
import aethereal.core.Skeleton;
import aethereal.lib.log4j.LogManager;
import aethereal.lib.log4j.Logger;
import aethereal.module.misc.DiscordRPC;
import aethereal.notification.Notification;
import com.google.gson.JsonObject;

public class DiscordProcessor extends BaseProcessor {
    private static final Logger LOGGER = LogManager.b(DiscordProcessor.class);
    // Public Discord RPC sample app. Handshake rejects unknown IDs.
    // Name is overridden in SET_ACTIVITY. Large image must be Discord CDN:
    // other hosts are accepted then proxied as "?", so we use the Malice server icon.
    private static final long CLIENT_ID = 345229890980937739L;
    private static final String DETAILS = "Malice Visuals";
    private static final String STATE = "Стиль, который говорит сам за себя";
    private static final String[] LARGE_IMAGES = {
            "https://cdn.discordapp.com/icons/1541044981966442569/402b2c24df0fd503b40dd7f8080f32a5.png?size=512",
            "https://cdn.discordapp.com/icons/1541044981966442569/402b2c24df0fd503b40dd7f8080f32a5.png"
    };
    private static final String LARGE_IMAGE_FALLBACK = "https://cdn.discordapp.com/icons/1541044981966442569/402b2c24df0fd503b40dd7f8080f32a5.png?size=512";

    private DiscordIPC ipc;
    private Thread worker;
    private final long startedAt = System.currentTimeMillis() / 1000L;
    private volatile boolean stopped;
    private volatile boolean moduleEnabled;
    private volatile boolean activitySent;
    private volatile boolean sending;
    private volatile String largeImage = LARGE_IMAGES[0];
    private volatile int largeImageIndex;

    @Override
    public void setup() {
        this.stopped = false;
        this.ipc = DiscordIPC.a(DiscordIPCConfig.a()
                .clientId(CLIENT_ID)
                .reconnect(true)
                .maxReconnectAttempts(0)
                .build());
        this.ipc.a(new DiscordEventListener() {
            @Override
            public void a(aethereal.core.DiscordUser user) {
                activitySent = false;
            }

            @Override
            public void a() {
                activitySent = false;
            }

            @Override
            public void b(int errorCode, String message) {
                activitySent = false;
            }
        });
        try {
            DiscordRPC rpc = Skeleton.getInstance().getModuleProcessor().t().getDiscordRpc();
            if (rpc != null && rpc.m()) {
                this.moduleEnabled = true;
            }
        } catch (Throwable ignored) {
        }
        this.worker = new Thread(this::runLoop, "malice-discord-rpc");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void unSetup() {
        this.stopped = true;
        if (this.worker != null) {
            this.worker.interrupt();
        }
        if (this.ipc != null) {
            try {
                this.ipc.close();
            } catch (Throwable throwable) {
                LOGGER.b("Discord RPC shutdown failed", throwable);
            }
            this.ipc = null;
        }
    }

    public DiscordIPC a() {
        return this.ipc;
    }

    public void b(boolean enabled) {
        this.moduleEnabled = enabled;
        this.activitySent = false;
    }

    public boolean c() {
        return this.moduleEnabled;
    }

    private void runLoop() {
        while (!this.stopped) {
            try {
                DiscordIPC client = this.ipc;
                if (client == null) {
                    return;
                }
                if (!this.moduleEnabled) {
                    clearActivity();
                    Thread.sleep(1000L);
                    continue;
                }
                if (!client.d()) {
                    try {
                        client.a();
                    } catch (Throwable error) {
                        LOGGER.b("Discord RPC: {}", rootMessage(error));
                        if (isAccessDenied(error) && DiscordPipeFix.b()) {
                            notifyFix();
                            Thread.sleep(12000L);
                            continue;
                        }
                        Thread.sleep(isAccessDenied(error) ? 8000L : 4000L);
                        continue;
                    }
                }
                if (!this.moduleEnabled) {
                    clearActivity();
                    continue;
                }
                if (client.d()) {
                    pushActivity(client);
                }
                Thread.sleep(this.activitySent ? 20000L : 2500L);
                this.activitySent = false;
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable throwable) {
                LOGGER.b("Discord RPC loop error", throwable);
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void clearActivity() {
        DiscordIPC client = this.ipc;
        if (client == null || !client.d()) {
            return;
        }
        try {
            client.h();
        } catch (Throwable throwable) {
            LOGGER.b("Discord RPC clear failed: {}", rootMessage(throwable));
        }
    }

    private void pushActivity(DiscordIPC client) {
        if (this.stopped || !this.moduleEnabled || this.activitySent || this.sending || client == null || !client.d()) {
            return;
        }
        this.sending = true;
        try {
            JsonObject data = client.a(activity());
            while (!hasLargeImage(data) && nextImage()) {
                data = client.a(activity());
            }
            if (!hasLargeImage(data) && !LARGE_IMAGE_FALLBACK.equals(this.largeImage)) {
                this.largeImage = LARGE_IMAGE_FALLBACK;
                data = client.a(activity());
            }
            this.activitySent = true;
            String shown = data != null && data.has("name") ? data.get("name").getAsString() : DETAILS;
            LOGGER.a("Discord RPC activity updated ({})", shown);
        } catch (Throwable error) {
            this.activitySent = false;
            LOGGER.b("Discord RPC update failed: {}", rootMessage(error));
            try {
                client.c();
            } catch (Throwable ignored) {
            }
        } finally {
            this.sending = false;
        }
    }

    private Activity activity() {
        return new Activity.a()
                .type(ActivityType.PLAYING)
                .b(DETAILS)
                .state(STATE)
                .startAt(this.startedAt)
                .largeImage(this.largeImage, DETAILS)
                .instance(true)
                .build();
    }

    private static boolean hasLargeImage(JsonObject data) {
        if (data == null || !data.has("assets") || !data.get("assets").isJsonObject()) {
            return false;
        }
        JsonObject assets = data.getAsJsonObject("assets");
        return assets.has("large_image") && !assets.get("large_image").isJsonNull()
                && !assets.get("large_image").getAsString().isBlank();
    }

    private boolean nextImage() {
        if (this.largeImageIndex + 1 >= LARGE_IMAGES.length) {
            return false;
        }
        this.largeImageIndex++;
        this.largeImage = LARGE_IMAGES[this.largeImageIndex];
        return true;
    }

    private static void notifyFix() {
        try {
            Skeleton.getInstance().getModuleProcessor().m().a(new Notification(
                    "o",
                    "Discord запущен от администратора. Подтверди UAC — перезапущу его без этих прав.",
                    10000
            ));
        } catch (Throwable ignored) {
        }
    }

    private static boolean isAccessDenied(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.nio.file.AccessDeniedException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("access denied") || lower.contains("access is denied") || lower.contains("отказано в доступе")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
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
