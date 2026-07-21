package dev.ironcraft.minecart.storage;

import dev.ironcraft.minecart.rail.CustomRailType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Stores custom rail type coordinates in the owning chunk's PDC. */
public final class CustomRailRegistry {
    private final Map<CustomRailType, NamespacedKey> chunkDataKeys = new EnumMap<>(CustomRailType.class);
    private final Map<ChunkKey, EnumMap<CustomRailType, Set<Integer>>> cache = new HashMap<>();

    public CustomRailRegistry() {
        for (CustomRailType type : CustomRailType.values()) {
            chunkDataKeys.put(type, new NamespacedKey("ironcraft", type.path() + 's'));
        }
    }

    public CustomRailType railTypeAt(Block block) {
        return block.getType() == Material.POWERED_RAIL ? storedTypeAt(block) : null;
    }

    public CustomRailType storedTypeAt(Block block) {
        int packed = RailPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        EnumMap<CustomRailType, Set<Integer>> chunkPositions = positions(block.getChunk());
        for (CustomRailType type : CustomRailType.values()) {
            if (chunkPositions.get(type).contains(packed)) {
                return type;
            }
        }
        return null;
    }

    public boolean containsLocation(Block block) {
        return storedTypeAt(block) != null;
    }

    public void mark(Block block, CustomRailType type) {
        Chunk chunk = block.getChunk();
        int packed = RailPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        EnumMap<CustomRailType, Set<Integer>> chunkPositions = positions(chunk);

        for (CustomRailType candidate : CustomRailType.values()) {
            Set<Integer> values = chunkPositions.get(candidate);
            boolean changed = candidate == type ? values.add(packed) : values.remove(packed);
            if (changed) {
                persist(chunk, candidate, values);
            }
        }
    }

    public boolean unmark(Block block) {
        Chunk chunk = block.getChunk();
        int packed = RailPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        EnumMap<CustomRailType, Set<Integer>> chunkPositions = positions(chunk);
        boolean removed = false;
        for (CustomRailType type : CustomRailType.values()) {
            Set<Integer> values = chunkPositions.get(type);
            if (values.remove(packed)) {
                persist(chunk, type, values);
                removed = true;
            }
        }
        return removed;
    }

    public void validate(Chunk chunk) {
        EnumMap<CustomRailType, Set<Integer>> chunkPositions = positions(chunk);
        for (CustomRailType type : CustomRailType.values()) {
            Set<Integer> values = chunkPositions.get(type);
            boolean changed = values.removeIf(packed -> {
                int y = RailPositionCodec.blockY(packed);
                return y < chunk.getWorld().getMinHeight()
                        || y >= chunk.getWorld().getMaxHeight()
                        || chunk.getBlock(
                                RailPositionCodec.localX(packed),
                                y,
                                RailPositionCodec.localZ(packed)
                        ).getType() != Material.POWERED_RAIL;
            });
            if (changed) {
                persist(chunk, type, values);
            }
        }
    }

    public void unload(Chunk chunk) {
        cache.remove(ChunkKey.of(chunk));
    }

    public void clearCache() {
        cache.clear();
    }

    private EnumMap<CustomRailType, Set<Integer>> positions(Chunk chunk) {
        return cache.computeIfAbsent(ChunkKey.of(chunk), ignored -> load(chunk));
    }

    private EnumMap<CustomRailType, Set<Integer>> load(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        EnumMap<CustomRailType, Set<Integer>> loaded = new EnumMap<>(CustomRailType.class);
        for (CustomRailType type : CustomRailType.values()) {
            int[] stored = pdc.get(chunkDataKeys.get(type), PersistentDataType.INTEGER_ARRAY);
            Set<Integer> values = new HashSet<>();
            if (stored != null) {
                Arrays.stream(stored).forEach(values::add);
            }
            loaded.put(type, values);
        }

        return loaded;
    }

    private void persist(Chunk chunk, CustomRailType type, Set<Integer> values) {
        persistRaw(chunk.getPersistentDataContainer(), type, values);
    }

    private void persistRaw(PersistentDataContainer pdc, CustomRailType type, Set<Integer> values) {
        NamespacedKey key = chunkDataKeys.get(type);
        if (values.isEmpty()) {
            pdc.remove(key);
            return;
        }
        int[] packed = values.stream().mapToInt(Integer::intValue).sorted().toArray();
        pdc.set(key, PersistentDataType.INTEGER_ARRAY, packed);
    }

    private record ChunkKey(UUID worldId, int x, int z) {
        private static ChunkKey of(Chunk chunk) {
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }
}
