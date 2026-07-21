package dev.ironcraft.minecart.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import dev.ironcraft.minecart.item.CustomRailItemFactory;
import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.ExplosionResult;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class CopperRailLifecycleListener implements Listener {
    private final Plugin plugin;
    private final CustomRailRegistry registry;
    private final CustomRailItemFactory items;
    private final Map<BlockKey, CustomRailType> pendingPhysicsDrops = new HashMap<>();

    public CopperRailLifecycleListener(Plugin plugin, CustomRailRegistry registry, CustomRailItemFactory items) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.POWERED_RAIL) {
            return;
        }
        CustomRailType type = items.typeOf(event.getItemInHand());
        if (type != null) {
            registry.mark(block, type);
        } else {
            registry.unmark(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (registry.railTypeAt(block) == null) {
            return;
        }
        // BlockDropItemEvent performs the normal drop replacement. This is the
        // cleanup fallback for creative mode or another plugin suppressing drops.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.POWERED_RAIL) {
                registry.unmark(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        handleExplosion(event, event.blockList(), event.getYield(), event.getExplosionResult());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        handleExplosion(event, event.blockList(), event.getYield(), event.getExplosionResult());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent event) {
        Block brokenBlock = event.getBlockState().getBlock();
        CustomRailType type = registry.storedTypeAt(brokenBlock);
        if (type == null) {
            return;
        }
        for (Item drop : event.getItems()) {
            ItemStack stack = drop.getItemStack();
            if (stack.getType() == Material.POWERED_RAIL && !items.isCustomRail(stack)) {
                drop.setItemStack(items.create(type, stack.getAmount()));
            }
        }
        registry.unmark(brokenBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakBlock(BlockBreakBlockEvent event) {
        Block block = event.getBlock();
        CustomRailType type = registry.storedTypeAt(block);
        if (type == null) {
            return;
        }
        List<ItemStack> replacements = new ArrayList<>();
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next();
            if (stack.getType() == Material.POWERED_RAIL && !items.isCustomRail(stack)) {
                replacements.add(items.create(type, stack.getAmount()));
                drops.remove();
            }
        }
        event.getDrops().addAll(replacements);
        registry.unmark(block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        Block block = event.getBlock();
        CustomRailType type = registry.storedTypeAt(block);
        if (type == null) {
            return;
        }
        BlockKey key = BlockKey.of(block);
        if (event.willDrop()) {
            pendingPhysicsDrops.put(key, type);
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingPhysicsDrops.remove(key);
            if (!event.isCancelled() && block.getType() != Material.POWERED_RAIL) {
                registry.unmark(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block root = event.getBlock();
        queueUnsupportedRail(root);
        queueUnsupportedRail(root.getRelative(BlockFace.UP));
        queueUnsupportedRail(root.getRelative(BlockFace.NORTH));
        queueUnsupportedRail(root.getRelative(BlockFace.SOUTH));
        queueUnsupportedRail(root.getRelative(BlockFace.EAST));
        queueUnsupportedRail(root.getRelative(BlockFace.WEST));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (stack.getType() != Material.POWERED_RAIL || items.isCustomRail(stack)) {
            return;
        }
        BlockKey location = BlockKey.of(event.getLocation().getBlock());
        CustomRailType type = pendingPhysicsDrops.get(location);
        if (type != null) {
            event.getEntity().setItemStack(items.create(type, stack.getAmount()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        registry.validate(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        registry.unload(event.getChunk());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (CustomRailType type : CustomRailType.values()) {
            event.getPlayer().discoverRecipe(type.key());
        }
    }

    private void handleExplosion(
            org.bukkit.event.Cancellable event,
            List<Block> blocks,
            float yield,
            ExplosionResult result
    ) {
        List<ExplodedRail> customRails = new ArrayList<>();
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            CustomRailType type = registry.railTypeAt(block);
            if (type == null) {
                continue;
            }
            iterator.remove();
            customRails.add(new ExplodedRail(block, type));
        }
        if (customRails.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || (result != ExplosionResult.DESTROY && result != ExplosionResult.DESTROY_WITH_DECAY)) {
                return;
            }
            for (ExplodedRail exploded : customRails) {
                Block block = exploded.block();
                if (registry.railTypeAt(block) != exploded.type()) {
                    continue;
                }
                Location dropLocation = block.getLocation();
                World world = block.getWorld();
                registry.unmark(block);
                block.setType(Material.AIR, true);

                boolean drops = result == ExplosionResult.DESTROY
                        || ThreadLocalRandom.current().nextFloat() < Math.max(0.0f, Math.min(1.0f, yield));
                if (drops && tileDrops(world)) {
                    world.dropItemNaturally(dropLocation, items.create(exploded.type(), 1));
                }
            }
        });
    }

    private void queueUnsupportedRail(Block block) {
        CustomRailType type = registry.railTypeAt(block);
        if (type == null || block.getBlockData().isSupported(block)) {
            return;
        }
        BlockKey key = BlockKey.of(block);
        if (pendingPhysicsDrops.putIfAbsent(key, type) != null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingPhysicsDrops.remove(key);
            if (block.getType() != Material.POWERED_RAIL) {
                registry.unmark(block);
            }
        });
    }

    private static boolean tileDrops(World world) {
        return world.getGameRuleValue(GameRules.BLOCK_DROPS);
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record ExplodedRail(Block block, CustomRailType type) {
    }
}
