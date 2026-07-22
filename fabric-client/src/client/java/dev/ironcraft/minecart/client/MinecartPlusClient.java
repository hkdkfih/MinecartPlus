package dev.ironcraft.minecart.client;

import dev.ironcraft.minecart.client.network.HandshakePayload;
import dev.ironcraft.minecart.client.network.HelloPayload;
import dev.ironcraft.minecart.client.network.RailSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.level.block.Blocks;

public final class MinecartPlusClient implements ClientModInitializer {
    public static final int PROTOCOL_VERSION = 1;
    public static final RailClientState RAILS = new RailClientState();

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(HandshakePayload.TYPE, HandshakePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RailSyncPayload.TYPE, RailSyncPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> {
            if (payload.protocolVersion() == PROTOCOL_VERSION) {
                context.responseSender().sendPacket(new HelloPayload(PROTOCOL_VERSION, payload.nonce()));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                RailSyncPayload.TYPE,
                (payload, context) -> RAILS.accept(payload.data(), context.client())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RAILS.clear(client));

        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bakeContext) ->
                bakeContext.state().is(Blocks.POWERED_RAIL) ? new CustomRailBlockModel(model) : model
        ));
    }
}
