# Copper Powered Rail Resource Pack Guide

This guide targets **Minecraft Java Edition 26.2**, **Paper 26.2**, and resource pack format **88.0**. It explains how to give the copper powered rail a custom inventory texture and what is—and is not—possible for its placed appearance.

No texture assets are included with the plugin. The steps below produce them from the vanilla 26.2 powered rail artwork.

## Important limitation: placed rails cannot read item NBT

The copper powered rail is a normal `minecraft:powered_rail` in the world. That is intentional: keeping the real block preserves vanilla rail shape, redstone updates, minecart tracking, collision, and client behavior.

A placed powered rail cannot retain arbitrary item NBT or a Paper `PersistentDataContainer`. Powered rails are not block entities, so there is nowhere on the individual block to store that data. A vanilla Java resource pack also cannot choose a block model from:

- item PDC or `minecraft:custom_data`;
- `minecraft:custom_model_data` from the item that placed it;
- a plugin's coordinate registry; or
- arbitrary NBT attached to a block-change packet.

Vanilla block models are selected from the block's registered state. A powered rail exposes its normal properties—such as `shape`, `powered`, and `waterlogged`—but it has no custom material property. Consequently, two dry powered rails with the same shape and powered state must use the same vanilla block model.

`minecraft:custom_model_data` can distinguish the **ItemStack** in an inventory, hand, item frame, dropped-item entity, or item display. It does not distinguish the placed block.

## How the plugin remembers copper rails

The plugin uses two persistent markers:

- The custom Java item carries the boolean PDC marker `ironcraftminecartaddon:copper_powered_rail`. The Custom Model Data value is also accepted as a compatibility fallback for translated inventory items that do not preserve the PDC marker.
- Each chunk stores its copper rail coordinates in the integer-array PDC entry `ironcraftminecartaddon:copper_powered_rails`.

Each coordinate occupies one integer:

```java
int packed = (y << 8) | ((z & 15) << 4) | (x & 15);
```

The fields are:

- bits 0–3: local X within the chunk;
- bits 4–7: local Z within the chunk; and
- bits 8–31: signed absolute block Y.

Decode it with:

```java
int localX = packed & 15;
int localZ = (packed >>> 4) & 15;
int y = packed >> 8; // Arithmetic shift restores signed Y.

int worldX = (chunk.getX() << 4) + localX;
int worldZ = (chunk.getZ() << 4) + localZ;
```

When the marked item is placed, the world receives a real powered rail and the plugin adds its coordinate to the chunk record. When that coordinate is broken or otherwise removed, the plugin removes the record and creates the correctly marked copper rail drop.

The plugin-created Java item normally contains both the PDC marker and the Custom Model Data value. After placement, the chunk coordinate record—not the item's former model data—is authoritative for the block's copper identity.

## Resource pack layout

Start with this minimal pack for the custom inventory/hand appearance:

```text
CopperPoweredRailPack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   └── items/
    │       └── powered_rail.json
    └── ironcraftminecartaddon/
        ├── models/
        │   └── item/
        │       └── copper_powered_rail.json
        └── textures/
            └── block/
                └── copper_powered_rail.png
```

The ZIP must contain `pack.mcmeta` and `assets` at its root. Do not put them inside an extra directory in the ZIP.

### `pack.mcmeta`

Minecraft 26.2 uses resource pack format 88.0. Modern pack metadata represents a full major/minor pack version as a two-integer array:

```json
{
  "pack": {
    "description": "IronCraft Minecart Addon copper rail visuals",
    "min_format": [88, 0],
    "max_format": [88, 0]
  }
}
```

## Make the copper texture in Blockbench

### 1. Extract the exact 26.2 vanilla references

Install and launch Minecraft Java 26.2 once, then open the 26.2 client JAR as a ZIP archive. Its usual location is:

- Windows: `%AppData%/.minecraft/versions/26.2/26.2.jar`
- macOS: `~/Library/Application Support/minecraft/versions/26.2/26.2.jar`
- Linux: `~/.minecraft/versions/26.2/26.2.jar`

Copy these files to a working folder. Do not edit the originals inside the game JAR.

```text
assets/minecraft/textures/block/powered_rail.png
assets/minecraft/textures/block/powered_rail_on.png

assets/minecraft/models/item/powered_rail.json
assets/minecraft/items/powered_rail.json

assets/minecraft/models/block/powered_rail.json
assets/minecraft/models/block/powered_rail_on.json
assets/minecraft/models/block/powered_rail_raised_ne.json
assets/minecraft/models/block/powered_rail_raised_sw.json
assets/minecraft/models/block/powered_rail_on_raised_ne.json
assets/minecraft/models/block/powered_rail_on_raised_sw.json
assets/minecraft/models/block/rail_flat.json
assets/minecraft/models/block/template_rail_raised_ne.json
assets/minecraft/models/block/template_rail_raised_sw.json
assets/minecraft/blockstates/powered_rail.json
```

The blockstate file is a useful reference for how the six model files are rotated into all twelve combinations of six rail shapes and two powered states.

### 2. Recolor without changing the geometry

The vanilla rail geometry is already correct. The cleanest result is a texture recolor, not a remodel.

1. Open Blockbench and create a **Java Block/Item** project.
2. Import `powered_rail.png` into the Textures panel. If you want a geometry preview, open `rail_flat.json` and attach the texture to its `#rail` texture slot.
3. Switch to **Paint** mode. Keep the canvas at its original 16×16 resolution.
4. Recolor only the gold rail pixels to a copper palette. Sampling colors from the vanilla `copper_block.png` or copper ingot artwork keeps the result consistent with Minecraft's palette.
5. Preserve every transparent pixel. Do not add a background, antialiasing, blur, interpolation, or semi-transparent edge pixels.
6. Leave the sleepers and the central redstone details recognizable. This makes the result read immediately as a powered rail.
7. Save the result as `copper_powered_rail.png` under:

   ```text
   assets/ironcraftminecartaddon/textures/block/copper_powered_rail.png
   ```

For a future placed-block or display overlay, repeat the same process with `powered_rail_on.png` and save it as:

```text
assets/ironcraftminecartaddon/textures/block/copper_powered_rail_on.png
```

Use the same copper palette in both images. Preserve the vanilla difference between the unpowered and powered redstone details.

### 3. Create the custom item model

Create `assets/ironcraftminecartaddon/models/item/copper_powered_rail.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "ironcraftminecartaddon:block/copper_powered_rail"
  }
}
```

This deliberately uses the recolored block texture as the flat inventory sprite, matching the way the vanilla rail item is presented.

## Select the item texture with Custom Model Data

The default plugin configuration uses float Custom Model Data value **2602001** at index 0. In Minecraft 26.2, item model selection belongs in the `assets/<namespace>/items` directory; do not use the old `overrides` array in a model file.

Create `assets/minecraft/items/powered_rail.json`:

```json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "entries": [
      {
        "threshold": 2602001,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraftminecartaddon:item/copper_powered_rail"
        }
      },
      {
        "threshold": 2602002,
        "model": {
          "type": "minecraft:model",
          "model": "minecraft:item/powered_rail"
        }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/powered_rail"
    }
  }
}
```

`minecraft:range_dispatch` uses thresholds rather than equality. The second threshold intentionally returns values at or above 2602002 to the vanilla model. With integer-valued model identifiers, only the interval `[2602001, 2602002)`—that is, the intended value 2602001—uses the copper model. Items without Custom Model Data and ordinary powered rails use the vanilla fallback.

If `item.custom-model-data` is changed in the plugin configuration, update this item definition to the same value. Integer-valued identifiers are recommended so an immediately following integer threshold can bound the custom interval cleanly.

Do not remove the fallback. Without it, an ordinary powered rail can render as the missing-model checkerboard.

On the Paper side, the corresponding component is a float list:

```java
CustomModelDataComponent component = meta.getCustomModelDataComponent();
component.setFloats(List.of(2602001.0F));
meta.setCustomModelDataComponent(component);
```

The normal Java recipe result must also retain the boolean PDC marker. The plugin recognizes model value 2602001 as a deliberate compatibility fallback for translated inventory items, but the PDC marker remains the primary Java item identity.

## Test the item pack

1. Place the pack folder or ZIP in the client's resource-pack directory.
2. Enable it above other packs that replace the powered rail item definition.
3. Use `F3+T` to reload resources after editing.
4. Obtain one ordinary powered rail and one copper powered rail from the plugin.
5. Confirm that the ordinary item remains vanilla and the marked copper item uses the recolor in inventory, in hand, and when dropped.
6. Place both items. Both placed blocks will still look like vanilla powered rails until one of the placed-visual approaches below is implemented. Their gameplay behavior remains distinct because the server uses the chunk coordinate record.

If the copper item is purple and black, check the namespace, singular/plural folder names, JSON syntax, ZIP root, and the game log. In particular, the modern selector goes in `assets/minecraft/items/powered_rail.json`, while the referenced render model goes in `assets/ironcraftminecartaddon/models/item/copper_powered_rail.json`.

## Options for the placed appearance

### Option A: globally replace powered rails

The simplest placed texture pack replaces:

```text
assets/minecraft/textures/block/powered_rail.png
assets/minecraft/textures/block/powered_rail_on.png
```

with copper versions at the same paths.

This changes **every** powered rail, including the faster normal gold rail. A vanilla resource pack cannot apply those replacements only at coordinates recorded by the plugin. This option is technically simple but does not visually distinguish the two rail types, so it is not recommended for the addon's intended design.

### Option B: future ItemDisplay overlay

A future plugin version can leave the real powered rail in place and spawn an `ItemDisplay` with a resource-pack model directly over each recorded copper rail.

Such an implementation needs custom display models for:

- flat, unpowered and powered rails;
- raised north-east, unpowered and powered rails; and
- raised south-west, unpowered and powered rails.

The plugin can reuse the six vanilla model parents and rotate them according to the real rail's `shape`, exactly as `assets/minecraft/blockstates/powered_rail.json` does. The custom models would point their `rail` texture to `ironcraftminecartaddon:block/copper_powered_rail` or `copper_powered_rail_on`.

The display should be offset upward by a very small amount so its copper pixels cover the gold rail without excessive Z-fighting. It must be updated when rail shape or redstone power changes and cleaned up when the rail is removed. This preserves the actual powered rail for physics and provides a graceful fallback—players without the pack still see a normal powered rail—but it adds one display entity per copper rail and may show slight edge overlap at some camera angles.

### Option C: client mod

A Fabric, NeoForge, or other client mod can add a custom rendering rule for powered rails and receive the plugin's material identity over an explicit synchronization channel. This can provide the cleanest per-instance result, but every player must install a compatible mod. Registering an entirely new block would additionally require a modded server rather than this Paper-only design. Both approaches are outside the scope of a vanilla-client addon.

### Why block-state spoofing is not the default

Spoofing a note block or another carrier block to the client requires hijacking one of that block's real states in the resource pack. The client then uses the carrier's collision and movement assumptions instead of a rail's, which can cause prediction or minecart-riding problems. It also requires resending or rewriting chunk and block-update packets. Keeping the real powered rail plus a non-colliding visual overlay is safer.

## Geyser and Bedrock clients

The rail speed, recipes, placement identity, drops, and other gameplay decisions run on the Paper server, so they can continue to work for players connecting through Geyser.

Java resource packs are not applied directly by Minecraft Bedrock Edition. The Java item-definition JSON and Java block/item models in this guide therefore do not provide Bedrock visuals. To distinguish the copper rail for Bedrock players, the server needs a separate Bedrock resource pack and a matching Geyser custom-item mapping/addon configuration. Those files should use the same logical item identity and model value, but their exact configuration is Geyser-version-specific.

Without that separate Bedrock visual mapping, gameplay still works, but a Bedrock player should expect the copper rail to look like its translated vanilla powered-rail fallback.

## References

- [Minecraft Java Edition 26.2 technical changes](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2) — confirms resource pack format 88.0.
- [Modern item model format](https://feedback.minecraft.net/hc/en-us/articles/31658318322957-Minecraft-Java-Edition-Snapshot-24w45a) — documents `assets/<namespace>/items`, `minecraft:custom_model_data`, and `minecraft:range_dispatch`.
- [Modern pack metadata format](https://feedback.minecraft.net/hc/en-us/articles/38407004270605-Minecraft-Java-Edition-Snapshot-25w31a) — documents major/minor `min_format` and `max_format` values.
- [Paper data component API](https://docs.papermc.io/paper/dev/data-component-api/) — covers item Custom Model Data components.
- [Paper display entities](https://docs.papermc.io/paper/dev/display-entities/) — background for a future `ItemDisplay` overlay.
