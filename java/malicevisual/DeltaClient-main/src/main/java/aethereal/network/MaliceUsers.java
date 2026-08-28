package aethereal.network;

import aethereal.config.ThemeInfo;
import aethereal.core.EventManager;
import aethereal.core.EventTarget;
import aethereal.core.Interface;
import aethereal.core.Skeleton;
import aethereal.event.PacketEvent;
import aethereal.event.TickEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaliceUsers implements Interface {
    public static final String MARK = "\uE111";
    private static final Identifier MARK_FONT = Identifier.of("skeleton", "prefixes");
    private static final MaliceUsers INSTANCE = new MaliceUsers();
    private static final long TTL_MS = 90000L;
    private static final int PING_INTERVAL = 80;

    private final Map<UUID, Long> users = new ConcurrentHashMap<>();
    private int ticks;

    private MaliceUsers() {
    }

    public static MaliceUsers get() {
        return INSTANCE;
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(MalicePingPayload.ID, MalicePingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MalicePingPayload.ID, MalicePingPayload.CODEC);
        try {
            ServerPlayNetworking.registerGlobalReceiver(MalicePingPayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                        if (ServerPlayNetworking.canSend(player, MalicePingPayload.ID)) {
                            ServerPlayNetworking.send(player, payload);
                        }
                    }
                });
            });
        } catch (Throwable ignored) {
        }
    }

    public void setup() {
        EventManager.a(this);
        ClientPlayNetworking.registerGlobalReceiver(MalicePingPayload.ID, (payload, context) -> {
            context.client().execute(() -> mark(payload.uuid()));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.player != null) {
                    mark(client.player.getUuid());
                    ping(client);
                }
            });
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            this.users.clear();
        });
    }

    public static boolean is(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && uuid.equals(client.player.getUuid())) {
            return true;
        }
        Long seen = get().users.get(uuid);
        return seen != null && System.currentTimeMillis() - seen < TTL_MS;
    }

    public static Text decorate(Text original) {
        if (original == null) {
            return original;
        }
        String raw = original.getString();
        if (raw.indexOf(MARK.charAt(0)) >= 0) {
            return original;
        }
        int purple = 0xE05CD0;
        try {
            purple = Skeleton.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor() & 0xFFFFFF;
        } catch (Throwable ignored) {
        }
        MutableText name = original.copy();
        name.fillStyle(Style.EMPTY.withColor(purple));
        return Text.empty()
                .append(Text.literal(MARK).setStyle(Style.EMPTY.withFont(MARK_FONT)))
                .append(Text.literal(" "))
                .append(name);
    }

    public static void mark(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return;
        }
        try {
            mark(UUID.fromString(uuid.trim()));
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void mark(UUID uuid) {
        if (uuid != null) {
            get().users.put(uuid, System.currentTimeMillis());
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        mark(client.player.getUuid());
        long now = System.currentTimeMillis();
        this.users.entrySet().removeIf(entry -> now - entry.getValue() > TTL_MS);
        this.ticks++;
        if (this.ticks % PING_INTERVAL == 0) {
            ping(client);
        }
    }

    private static void ping(MinecraftClient client) {
        if (client.player == null || !ClientPlayNetworking.canSend(MalicePingPayload.ID)) {
            return;
        }
        ClientPlayNetworking.send(new MalicePingPayload(client.player.getUuid().toString()));
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isReceive() || !(event.getPacket() instanceof CustomPayloadS2CPacket packet)) {
            return;
        }
        CustomPayload payload = packet.payload();
        if (payload instanceof MalicePingPayload ping) {
            mark(ping.uuid());
        }
    }
}
