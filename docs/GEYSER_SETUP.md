# Geyser compatibility and optional Bedrock visuals

MinecartPlus gameplay is server-authoritative and works through Geyser without a Geyser API dependency. Crafting, placement identity, drops, redstone behavior, and all six speed targets run on Paper.

Without extra files, Bedrock players see each custom item/block as a normal powered rail. That fallback is fully playable. The optional setup below gives the five custom **items** distinct inventory/hand icons; placed blocks remain powered rails.

## Enable custom content

Use a current Geyser build compatible with the server and enable custom content in Geyser's `config.yml`, then restart it:

```yaml
gameplay:
  enable-custom-content: true
```

Geyser does not directly use a Java resource pack, so create a separate Bedrock resource pack.

## Add Geyser v2 mappings

Create `minecartplus_rails.json` in Geyser's `custom_mappings` directory:

```json
{
  "format_version": 2,
  "items": {
    "minecraft:powered_rail": [
      {
        "type": "legacy",
        "custom_model_data": 2602000,
        "bedrock_identifier": "ironcraft:stone_powered_rail",
        "display_name": "Stone Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:stone_powered_rail",
          "creative_category": "items"
        }
      },
      {
        "type": "legacy",
        "custom_model_data": 2602001,
        "bedrock_identifier": "ironcraft:copper_powered_rail",
        "display_name": "Copper Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:copper_powered_rail",
          "creative_category": "items"
        }
      },
      {
        "type": "legacy",
        "custom_model_data": 2602002,
        "bedrock_identifier": "ironcraft:iron_powered_rail",
        "display_name": "Iron Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:iron_powered_rail",
          "creative_category": "items"
        }
      },
      {
        "type": "legacy",
        "custom_model_data": 2602003,
        "bedrock_identifier": "ironcraft:diamond_powered_rail",
        "display_name": "Diamond Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:diamond_powered_rail",
          "creative_category": "items"
        }
      },
      {
        "type": "legacy",
        "custom_model_data": 2602004,
        "bedrock_identifier": "ironcraft:netherite_powered_rail",
        "display_name": "Netherite Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:netherite_powered_rail",
          "creative_category": "items"
        }
      }
    ]
  }
}
```

Typical mapping locations:

- Paper plugin: `plugins/Geyser-Spigot/custom_mappings/`
- Standalone Geyser: `custom_mappings/`

If a value under `item.*-custom-model-data` changes in MinecartPlus's `config.yml`, change the matching mapping too.

## Add the Bedrock pack

Create a standard Bedrock resource pack with unique header/module UUIDs in `manifest.json`. Its `textures/item_texture.json` should contain:

```json
{
  "resource_pack_name": "minecartplus_rails",
  "texture_name": "atlas.items",
  "texture_data": {
    "ironcraft:stone_powered_rail": {
      "textures": ["textures/items/stone_powered_rail"]
    },
    "ironcraft:copper_powered_rail": {
      "textures": ["textures/items/copper_powered_rail"]
    },
    "ironcraft:iron_powered_rail": {
      "textures": ["textures/items/iron_powered_rail"]
    },
    "ironcraft:diamond_powered_rail": {
      "textures": ["textures/items/diamond_powered_rail"]
    },
    "ironcraft:netherite_powered_rail": {
      "textures": ["textures/items/netherite_powered_rail"]
    }
  }
}
```

Add these Bedrock-format PNGs:

```text
textures/items/copper_powered_rail.png
textures/items/iron_powered_rail.png
textures/items/diamond_powered_rail.png
textures/items/stone_powered_rail.png
textures/items/netherite_powered_rail.png
```

Put the finished `.zip` or `.mcpack` in Geyser's packs directory and restart Geyser:

- Paper plugin: `plugins/Geyser-Spigot/packs/`
- Standalone Geyser: `packs/`

The recoloring workflow in [RESOURCE_PACK_GUIDE.md](RESOURCE_PACK_GUIDE.md) applies to these PNGs too.

## Placed blocks

All tiers remain real `minecraft:powered_rail` blocks for Java and Bedrock. Geyser cannot select a different placed model from MinecartPlus's private coordinate record, so these mappings change only item icons. Per-tier placed visuals require a future display-overlay implementation or a purpose-built client/Geyser extension.

References: [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/) and [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/).
