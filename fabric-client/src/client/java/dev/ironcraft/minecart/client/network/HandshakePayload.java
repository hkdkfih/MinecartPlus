package dev.ironcraft.minecart.client.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HandshakePayload(int protocolVersion, int nonce) implements CustomPacketPayload {
    public static final Type<HandshakePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ironcraft", "rail_handshake")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> CODEC = StreamCodec.ofMember(
            HandshakePayload::write,
            HandshakePayload::read
    );

    private static HandshakePayload read(RegistryFriendlyByteBuf buffer) {
        return new HandshakePayload(buffer.readUnsignedByte(), buffer.readInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(protocolVersion);
        buffer.writeInt(nonce);
    }

    @Override
    public Type<HandshakePayload> type() {
        return TYPE;
    }
}
