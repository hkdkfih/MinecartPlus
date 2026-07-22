# MinecartPlus resource packs

## Ready-to-upload files

- `dist/MinecartPlus-Java-26.2.zip`: Minecraft Java 26.2 resource pack.
- `dist/MinecartPlus-Bedrock.mcpack`: Bedrock resource pack for Geyser.
- `geyser/minecartplus_rails.json`: Geyser v2 custom-item mappings.

## Java server

Host `MinecartPlus-Java-26.2.zip` at a direct HTTPS download URL, then set that URL as `resource-pack` in `server.properties`. Set `resource-pack-sha1` to the archive's SHA-1 value. The ZIP has `pack.mcmeta` and `assets/` directly at its root.

Current Java ZIP SHA-1:

```text
ec247399d89331a9770526c390573bcfff98ad6b
```

Archive SHA-256 values:

```text
0827ac22e605f617bcf7eb64140e4ffda04e2af7cb05d3d520d0754a5b4bbbcd  MinecartPlus-Java-26.2.zip
47644d5e3c7d42a3edce597e473a0a218de978cc3bb412435b6784541eb07599  MinecartPlus-Bedrock.mcpack
```

## Geyser/Bedrock server

1. Enable `gameplay.enable-custom-content: true` in Geyser's `config.yml`.
2. Copy `MinecartPlus-Bedrock.mcpack` to `plugins/Geyser-Spigot/packs/` (or standalone Geyser's `packs/`).
3. Copy `minecartplus_rails.json` to `plugins/Geyser-Spigot/custom_mappings/` (or standalone Geyser's `custom_mappings/`).
4. Restart Geyser.

The packs use Custom Model Data values `2602000` through `2602004`, matching MinecartPlus's default configuration.

## Supplied texture notes

- The source file `blockbench/diamond_rail_on.png` is packaged under the normalized name `diamond_powered_rail_on.png`.
- The updated copper and iron `_on` textures are distinct and are packaged as supplied.
- All ten off/on textures are included in both source packs. Current item routing references the five non-`_on` textures.
