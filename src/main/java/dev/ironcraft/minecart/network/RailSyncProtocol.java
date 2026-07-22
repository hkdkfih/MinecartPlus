package dev.ironcraft.minecart.network;

import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Binary protocol shared with the optional MinecartPlus Fabric client. */
public final class RailSyncProtocol {
    public static final int VERSION = 1;
    public static final String HANDSHAKE_CHANNEL = "ironcraft:rail_handshake";
    public static final String HELLO_CHANNEL = "ironcraft:rail_hello";
    public static final String SYNC_CHANNEL = "ironcraft:rail_sync";

    static final int OP_CLEAR_ALL = 0;
    static final int OP_CHUNK_SNAPSHOT = 1;
    static final int OP_FORGET_CHUNK = 2;
    static final int OP_UPDATE = 3;
    static final int MAX_ENTRIES_PER_SNAPSHOT = 4_000;

    private RailSyncProtocol() {
    }

    public static byte[] handshakeChallenge(int nonce) {
        return packet(output -> output.writeInt(nonce));
    }

    public static byte[] clearAll() {
        return packet(output -> output.writeByte(OP_CLEAR_ALL));
    }

    public static List<byte[]> chunkSnapshot(
            String worldKey,
            int chunkX,
            int chunkZ,
            List<CustomRailRegistry.StoredRail> rails
    ) {
        List<byte[]> packets = new ArrayList<>();
        if (rails.isEmpty()) {
            packets.add(snapshotFragment(worldKey, chunkX, chunkZ, true, List.of()));
            return packets;
        }

        for (int start = 0; start < rails.size(); start += MAX_ENTRIES_PER_SNAPSHOT) {
            int end = Math.min(start + MAX_ENTRIES_PER_SNAPSHOT, rails.size());
            packets.add(snapshotFragment(worldKey, chunkX, chunkZ, start == 0, rails.subList(start, end)));
        }
        return packets;
    }

    public static byte[] forgetChunk(String worldKey, int chunkX, int chunkZ) {
        return packet(output -> {
            output.writeByte(OP_FORGET_CHUNK);
            writeString(output, worldKey);
            output.writeInt(chunkX);
            output.writeInt(chunkZ);
        });
    }

    public static byte[] update(String worldKey, int x, int y, int z, CustomRailType type) {
        return packet(output -> {
            output.writeByte(OP_UPDATE);
            writeString(output, worldKey);
            output.writeInt(x);
            output.writeInt(y);
            output.writeInt(z);
            output.writeByte(networkId(type));
        });
    }

    private static byte[] snapshotFragment(
            String worldKey,
            int chunkX,
            int chunkZ,
            boolean replace,
            List<CustomRailRegistry.StoredRail> rails
    ) {
        return packet(output -> {
            output.writeByte(OP_CHUNK_SNAPSHOT);
            writeString(output, worldKey);
            output.writeInt(chunkX);
            output.writeInt(chunkZ);
            output.writeBoolean(replace);
            output.writeShort(rails.size());
            for (CustomRailRegistry.StoredRail rail : rails) {
                output.writeInt(rail.packedPosition());
                output.writeByte(networkId(rail.type()));
            }
        });
    }

    private static int networkId(CustomRailType type) {
        if (type == null) {
            return 0;
        }
        return switch (type) {
            case COPPER -> 1;
            case IRON -> 2;
            case DIAMOND -> 3;
        };
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65_535) {
            throw new IllegalArgumentException("World key is too long for the rail sync protocol");
        }
        output.writeShort(bytes.length);
        output.write(bytes);
    }

    private static byte[] packet(PacketWriter writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeByte(VERSION);
                writer.write(output);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @FunctionalInterface
    private interface PacketWriter {
        void write(DataOutputStream output) throws IOException;
    }
}
