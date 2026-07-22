package dev.ironcraft.minecart.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RailSyncPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<RailSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ironcraft", "rail_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RailSyncPayload> CODEC = StreamCodec.ofMember(
            RailSyncPayload::write,
            RailSyncPayload::read
    );

    public RailSyncPayload {
        data = data.clone();
    }

    private static RailSyncPayload read(RegistryFriendlyByteBuf buffer) {
        byte[] data = new byte[buffer.readableBytes()];
        buffer.readBytes(data);
        return new RailSyncPayload(data);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBytes(data);
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public Type<RailSyncPayload> type() {
        return TYPE;
    }
}
