package dev.ironcraft.minecart.client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

public final class RailClientState {
    private static final int PROTOCOL_VERSION = 1;
    private static final int OP_CLEAR_ALL = 0;
    private static final int OP_CHUNK_SNAPSHOT = 1;
    private static final int OP_FORGET_CHUNK = 2;
    private static final int OP_UPDATE = 3;
    private static final int MAX_WORLD_KEY_BYTES = 512;
    private static final int MAX_SNAPSHOT_ENTRIES = 4_000;

    private final ConcurrentMap<Identifier, ConcurrentMap<Long, Map<Integer, RailType>>> dimensions =
            new ConcurrentHashMap<>();

    public RailType typeAt(Identifier dimension, BlockPos position) {
        Map<Long, Map<Integer, RailType>> chunks = dimensions.get(dimension);
        if (chunks == null) {
            return null;
        }
        Map<Integer, RailType> rails = chunks.get(ChunkPos.pack(position.getX() >> 4, position.getZ() >> 4));
        return rails == null ? null : rails.get(pack(position.getX(), position.getY(), position.getZ()));
    }

    public void accept(byte[] packet, Minecraft client) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet))) {
            int version = input.readUnsignedByte();
            if (version != PROTOCOL_VERSION) {
                return;
            }
            switch (input.readUnsignedByte()) {
                case OP_CLEAR_ALL -> clear(client);
                case OP_CHUNK_SNAPSHOT -> readSnapshot(input, client);
                case OP_FORGET_CHUNK -> readForget(input, client);
                case OP_UPDATE -> readUpdate(input, client);
                default -> { }
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // Ignore malformed or newer protocol messages without affecting play.
        }
    }

    public void clear(Minecraft client) {
        ClientLevel level = client.level;
        if (level != null) {
            Identifier current = level.dimension().identifier();
            Map<Long, Map<Integer, RailType>> removed = dimensions.remove(current);
            dirtyChunks(client, current, removed);
        }
        dimensions.clear();
    }

    private void readSnapshot(DataInputStream input, Minecraft client) throws IOException {
        Identifier dimension = readIdentifier(input);
        int chunkX = input.readInt();
        int chunkZ = input.readInt();
        boolean replace = input.readBoolean();
        int count = input.readUnsignedShort();
        if (count > MAX_SNAPSHOT_ENTRIES) {
            throw new IOException("Rail snapshot is too large");
        }

        Map<Integer, RailType> fragment = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int packed = input.readInt();
            RailType type = RailType.fromNetworkId(input.readUnsignedByte());
            if (type == null) {
                throw new IOException("Unknown rail type");
            }
            fragment.put(packed, type);
        }

        ConcurrentMap<Long, Map<Integer, RailType>> chunks = dimensions.computeIfAbsent(
                dimension,
                ignored -> new ConcurrentHashMap<>()
        );
        long chunkKey = ChunkPos.pack(chunkX, chunkZ);
        Map<Integer, RailType> before = chunks.get(chunkKey);
        chunks.compute(chunkKey, (ignored, current) -> {
            Map<Integer, RailType> combined = replace ? new HashMap<>() : new HashMap<>(current == null ? Map.of() : current);
            combined.putAll(fragment);
            return combined.isEmpty() ? null : Map.copyOf(combined);
        });
        dirtyRails(client, dimension, chunkX, chunkZ, before);
        dirtyRails(client, dimension, chunkX, chunkZ, chunks.get(chunkKey));
    }

    private void readForget(DataInputStream input, Minecraft client) throws IOException {
        Identifier dimension = readIdentifier(input);
        int chunkX = input.readInt();
        int chunkZ = input.readInt();
        Map<Long, Map<Integer, RailType>> chunks = dimensions.get(dimension);
        if (chunks == null) {
            return;
        }
        Map<Integer, RailType> removed = chunks.remove(ChunkPos.pack(chunkX, chunkZ));
        dirtyRails(client, dimension, chunkX, chunkZ, removed);
    }

    private void readUpdate(DataInputStream input, Minecraft client) throws IOException {
        Identifier dimension = readIdentifier(input);
        int x = input.readInt();
        int y = input.readInt();
        int z = input.readInt();
        int networkId = input.readUnsignedByte();
        RailType type = RailType.fromNetworkId(networkId);
        if (networkId != 0 && type == null) {
            throw new IOException("Unknown rail type");
        }

        ConcurrentMap<Long, Map<Integer, RailType>> chunks = dimensions.computeIfAbsent(
                dimension,
                ignored -> new ConcurrentHashMap<>()
        );
        long chunkKey = ChunkPos.pack(x >> 4, z >> 4);
        int packed = pack(x, y, z);
        chunks.compute(chunkKey, (ignored, current) -> {
            Map<Integer, RailType> updated = new HashMap<>(current == null ? Map.of() : current);
            if (type == null) {
                updated.remove(packed);
            } else {
                updated.put(packed, type);
            }
            return updated.isEmpty() ? null : Map.copyOf(updated);
        });
        dirtyPosition(client, dimension, new BlockPos(x, y, z));
    }

    private static Identifier readIdentifier(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > MAX_WORLD_KEY_BYTES) {
            throw new IOException("World key is too long");
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Truncated world key");
        }
        Identifier identifier = Identifier.tryParse(new String(bytes, StandardCharsets.UTF_8));
        if (identifier == null) {
            throw new IOException("Invalid world key");
        }
        return identifier;
    }

    private static void dirtyChunks(
            Minecraft client,
            Identifier dimension,
            Map<Long, Map<Integer, RailType>> chunks
    ) {
        if (chunks == null) {
            return;
        }
        for (Map.Entry<Long, Map<Integer, RailType>> entry : chunks.entrySet()) {
            dirtyRails(client, dimension, ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), entry.getValue());
        }
    }

    private static void dirtyRails(
            Minecraft client,
            Identifier dimension,
            int chunkX,
            int chunkZ,
            Map<Integer, RailType> rails
    ) {
        if (rails == null || client.level == null || !client.level.dimension().identifier().equals(dimension)) {
            return;
        }
        Set<Integer> sectionYs = new HashSet<>();
        for (int packed : rails.keySet()) {
            sectionYs.add((packed >> 8) >> 4);
        }
        for (int sectionY : sectionYs) {
            client.levelExtractor.setSectionDirty(chunkX, sectionY, chunkZ);
        }
    }

    private static void dirtyPosition(Minecraft client, Identifier dimension, BlockPos position) {
        if (client.level != null && client.level.dimension().identifier().equals(dimension)) {
            client.levelExtractor.setSectionDirty(position.getX() >> 4, position.getY() >> 4, position.getZ() >> 4);
        }
    }

    private static int pack(int x, int y, int z) {
        return (y << 8) | ((z & 15) << 4) | (x & 15);
    }
}
