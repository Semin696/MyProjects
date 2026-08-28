package aethereal.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MalicePingPayload(String uuid) implements CustomPayload {
    public static final Id<MalicePingPayload> ID = new Id<>(Identifier.of("malice", "s"));
    public static final PacketCodec<RegistryByteBuf, MalicePingPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, MalicePingPayload::uuid, MalicePingPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
