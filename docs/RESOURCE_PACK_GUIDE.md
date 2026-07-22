# MinecartPlus Rail Resource Pack and Blockbench Guide

This guide targets Minecraft Java 26.2, Paper 26.2, and resource pack format 88.0. The finished textures are stored in `blockbench/` and bundled by the optional Fabric companion; the steps below explain the standalone item resource pack and how those textures were made.

## What a resource pack can change

The custom tiers are real `minecraft:powered_rail` items and blocks. Custom Model Data can give the **item** a distinct model in inventories, hands, item frames, dropped-item entities, and item displays.

A vanilla resource pack cannot distinguish a placed MinecartPlus rail. Powered rails are not block entities, so placed blocks cannot retain item NBT/PDC or Custom Model Data. Their client model is selected only from normal states such as shape and powered. Two powered rails with the same state must therefore use the same placed model.

MinecartPlus remembers placed types in server-side chunk data:

- item IDs: `ironcraft:stone_powered_rail`, `ironcraft:copper_powered_rail`, `ironcraft:iron_powered_rail`, `ironcraft:diamond_powered_rail`, and `ironcraft:netherite_powered_rail`;
- generic item marker: `ironcraft:rail_id`;
- chunk coordinate lists use the corresponding plural logical IDs for all five tiers.

This server-side spoof preserves vanilla rail shapes, redstone, collision, minecart tracking, Java-client compatibility, and Geyser compatibility.

## Pack layout

```text
MinecartPlusRailPack/
├── pack.mcmeta
└── assets/
    ├── minecraft/
    │   └── items/
    │       └── powered_rail.json
    └── ironcraft/
        ├── models/
        │   └── item/
        │       ├── stone_powered_rail.json
        │       ├── copper_powered_rail.json
        │       ├── iron_powered_rail.json
        │       ├── diamond_powered_rail.json
        │       └── netherite_powered_rail.json
        └── textures/
            └── block/
                ├── stone_powered_rail.png
                ├── copper_powered_rail.png
                ├── iron_powered_rail.png
                ├── diamond_powered_rail.png
                └── netherite_powered_rail.png
```

The ZIP must contain `pack.mcmeta` and `assets` at its root, not inside an extra folder.

### `pack.mcmeta`

```json
{
  "pack": {
    "description": "MinecartPlus rail visuals",
    "min_format": [88, 0],
    "max_format": [88, 0]
  }
}
```

## Create the textures in Blockbench

### 1. Extract the vanilla references

Launch Java 26.2 once, then open the 26.2 client JAR as a ZIP. It is normally located at:

- Windows: `%AppData%/.minecraft/versions/26.2/26.2.jar`
- macOS: `~/Library/Application Support/minecraft/versions/26.2/26.2.jar`
- Linux: `~/.minecraft/versions/26.2/26.2.jar`

Copy these references into a working folder:

```text
assets/minecraft/textures/block/powered_rail.png
assets/minecraft/textures/block/powered_rail_on.png
assets/minecraft/models/item/powered_rail.json
assets/minecraft/items/powered_rail.json
assets/minecraft/models/block/rail_flat.json
assets/minecraft/blockstates/powered_rail.json
```

Do not edit files inside the game JAR.

### 2. Recolor the normal powered rail

1. In Blockbench, create a **Java Block/Item** project.
2. Import `powered_rail.png` in the Textures panel. For a geometry preview, open `rail_flat.json` and assign the image to its `#rail` texture slot.
3. Switch to Paint mode and retain the original 16×16 size.
4. Select only the gold rail pixels. Recolor them using colors sampled from Minecraft's stone, copper, iron, diamond, or netherite item/block textures.
5. Preserve light, mid, and shadow tones; hue-shifting the existing shades works better than painting one flat color.
6. Do not alter transparent pixels, sleepers, or the central redstone detail. Disable smoothing, interpolation, antialiasing, and blur.
7. Export one PNG per tier to the five texture paths shown above.

For possible future placed/display visuals, repeat the process with `powered_rail_on.png` and export `<tier>_powered_rail_on.png`. Keep the vanilla powered/unpowered redstone difference.

### 3. Create the item models

Create `assets/ironcraft/models/item/copper_powered_rail.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "ironcraft:block/copper_powered_rail"
  }
}
```

Duplicate it for the other four tiers, changing `copper` in `layer0` to the relevant material.

## Route Custom Model Data to the models

MinecartPlus uses these float values at Custom Model Data index 0:

| Tier | Value |
|---|---:|
| Stone | 2602000 |
| Copper | 2602001 |
| Iron | 2602002 |
| Diamond | 2602003 |
| Netherite | 2602004 |

Create `assets/minecraft/items/powered_rail.json`:

```json
{
  "model": {
    "type": "minecraft:range_dispatch",
    "property": "minecraft:custom_model_data",
    "index": 0,
    "entries": [
      {
        "threshold": 2602000,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraft:item/stone_powered_rail"
        }
      },
      {
        "threshold": 2602001,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraft:item/copper_powered_rail"
        }
      },
      {
        "threshold": 2602002,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraft:item/iron_powered_rail"
        }
      },
      {
        "threshold": 2602003,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraft:item/diamond_powered_rail"
        }
      },
      {
        "threshold": 2602004,
        "model": {
          "type": "minecraft:model",
          "model": "ironcraft:item/netherite_powered_rail"
        }
      },
      {
        "threshold": 2602005,
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

`range_dispatch` uses intervals, not equality. The final 2602005 entry closes the netherite interval and returns later values to vanilla. The fallback keeps items without Custom Model Data—including normal powered rails—vanilla.

If you change an `item.*-custom-model-data` setting in `config.yml`, update the matching threshold. Keeping the five values consecutive makes the interval boundaries straightforward.

## Test the Java pack

1. Place the pack folder/ZIP in the client resource-pack directory and enable it above packs that replace powered rails.
2. Press `F3+T` after edits.
3. Obtain a vanilla powered rail and all five MinecartPlus tiers.
4. Verify the normal item remains gold and each custom item has its recolor in inventory, hand, and dropped form.
5. Place the items. With only the resource pack, all six placed blocks still look vanilla. Install the optional Fabric companion to render each recorded tier distinctly.

For purple/black models, check JSON syntax, the ZIP root, singular `items` versus `models/item`, the `ironcraft` namespace, and the client log.

## Options for distinct placed visuals

- **Global texture replacement:** replacing Minecraft's two powered-rail block textures changes every tier, so it cannot distinguish them.
- **Future ItemDisplay overlay:** MinecartPlus could spawn a non-colliding custom display over each recorded rail, synchronized with rail shape and redstone power. This preserves the real rail but costs one display entity per custom rail.
- **Included Fabric companion:** build `fabric-client/` or install its JAR to receive authenticated coordinate/type data and render the supplied off/on sprite per rail. See [Fabric client setup](FABRIC_CLIENT.md).

Hijacking an unrelated carrier block state is not used because its collision and client minecart assumptions can differ from a real rail.

## Bedrock/Geyser

Java resource packs and the Fabric client mod do not supply Bedrock visuals. Gameplay still works through Geyser, while optional Bedrock item icons require the mappings and Bedrock pack described in [GEYSER_SETUP.md](GEYSER_SETUP.md). Placed tiers remain visually identical on Bedrock.

References:

- [Minecraft Java Edition 26.2 technical changes](https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2)
- [Modern item model format](https://feedback.minecraft.net/hc/en-us/articles/31658318322957-Minecraft-Java-Edition-Snapshot-24w45a)
- [Paper data component API](https://docs.papermc.io/paper/dev/data-component-api/)
- [Paper display entities](https://docs.papermc.io/paper/dev/display-entities/)
