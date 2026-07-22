package dev.ironcraft.minecart.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HelloPayload(int protocolVersion, int nonce) implements CustomPacketPayload {
    public static final Type<HelloPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ironcraft", "rail_hello")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = StreamCodec.ofMember(
            HelloPayload::write,
            HelloPayload::read
    );

    private static HelloPayload read(RegistryFriendlyByteBuf buffer) {
        return new HelloPayload(buffer.readUnsignedByte(), buffer.readInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(protocolVersion);
        buffer.writeInt(nonce);
    }

    @Override
    public Type<HelloPayload> type() {
        return TYPE;
    }
}
