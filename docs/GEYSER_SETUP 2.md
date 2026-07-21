# Geyser compatibility and optional Bedrock visuals

The plugin's gameplay is server-authoritative and works for players connecting through Geyser. No Geyser API dependency is required. Crafting, placement, chunk-persistent copper identity, custom drops, redstone behavior, and minecart target speed all run on Paper.

Without extra visual files, Geyser translates the item and block as a normal powered rail. That fallback is fully playable. The setup below is optional and only gives the **inventory/hand item** a copper icon.

## Current requirements

Use a current Geyser build compatible with Minecraft 26.2. In Geyser's `config.yml`, enable custom content and restart Geyser:

```yaml
gameplay:
  enable-custom-content: true
```

Geyser does not convert Java resource packs to Bedrock packs. A separate Bedrock resource pack is required.

## Add the Geyser v2 item mapping

The default plugin configuration puts Custom Model Data float `2602001` at index 0 on the Copper Powered Rail. Create a JSON file such as `ironcraft_copper_rail.json` in Geyser's `custom_mappings` directory:

```json
{
  "format_version": 2,
  "items": {
    "minecraft:powered_rail": [
      {
        "type": "legacy",
        "custom_model_data": 2602001,
        "bedrock_identifier": "ironcraft:copper_powered_rail",
        "display_name": "Copper Powered Rail",
        "bedrock_options": {
          "icon": "ironcraft:copper_powered_rail",
          "creative_category": "items"
        }
      }
    ]
  }
}
```

`creative_category` is included because Geyser requires a category for custom recipe outputs to appear properly in the Bedrock recipe book.

Typical locations are:

- Paper plugin: `plugins/Geyser-Spigot/custom_mappings/`
- Geyser standalone: `custom_mappings/`

If `item.custom-model-data` is changed in this plugin's `config.yml`, update the mapping to exactly the same value.

## Add the Bedrock resource pack

Create a standard Bedrock resource pack with unique header/module UUIDs in `manifest.json`. Add this `textures/item_texture.json` entry:

```json
{
  "resource_pack_name": "ironcraft_copper_rail",
  "texture_name": "atlas.items",
  "texture_data": {
    "ironcraft:copper_powered_rail": {
      "textures": [
        "textures/items/copper_powered_rail"
      ]
    }
  }
}
```

Place a Bedrock-format PNG at:

```text
textures/items/copper_powered_rail.png
```

The recoloring advice in `RESOURCE_PACK_GUIDE.md` is still useful, but export/copy the finished pixel art into this Bedrock path. Put the finished `.zip` or `.mcpack` in Geyser's `packs` directory and restart Geyser:

- Paper plugin: `plugins/Geyser-Spigot/packs/`
- Geyser standalone: `packs/`

## Placed copper rail appearance

The placed block remains a real `minecraft:powered_rail` for Java and Bedrock clients. Geyser cannot select a different placed model from the plugin's private coordinate record, so the optional mapping changes only the item icon. Distinct placed visuals would require a future display-overlay implementation or a purpose-built Geyser extension/resource-pack system; they are not required for correct gameplay.

Current official references: [Geyser custom items](https://geysermc.org/wiki/geyser/custom-items/) and [Geyser resource packs](https://geysermc.org/wiki/geyser/packs/).
