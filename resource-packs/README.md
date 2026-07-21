# MinecartPlus resource packs

## Ready-to-upload files

- `dist/MinecartPlus-Java-26.2.zip`: Minecraft Java 26.2 resource pack.
- `dist/MinecartPlus-Bedrock.mcpack`: Bedrock resource pack for Geyser.
- `geyser/minecartplus_rails.json`: Geyser v2 custom-item mappings.

## Java server

Host `MinecartPlus-Java-26.2.zip` at a direct HTTPS download URL, then set that URL as `resource-pack` in `server.properties`. Set `resource-pack-sha1` to the archive's SHA-1 value. The ZIP has `pack.mcmeta` and `assets/` directly at its root.

Current Java ZIP SHA-1:

```text
e629524860699bbf26ae7e93e2475b8f24a67b56
```

Archive SHA-256 values:

```text
ef39e04f83b97cc8210e4388a6921e6cf2519389399a76695dba40c5598556d7  MinecartPlus-Java-26.2.zip
9dc97caacaeb9824b72a979388e583f033fd1a47a9c5d7c99882eda21a3f9498  MinecartPlus-Bedrock.mcpack
```

## Geyser/Bedrock server

1. Enable `gameplay.enable-custom-content: true` in Geyser's `config.yml`.
2. Copy `MinecartPlus-Bedrock.mcpack` to `plugins/Geyser-Spigot/packs/` (or standalone Geyser's `packs/`).
3. Copy `minecartplus_rails.json` to `plugins/Geyser-Spigot/custom_mappings/` (or standalone Geyser's `custom_mappings/`).
4. Restart Geyser.

The packs use Custom Model Data values `2602001`, `2602002`, and `2602003`, matching MinecartPlus's default configuration.

## Supplied texture notes

- The source file `blockbench/diamond_rail_on.png` is packaged under the normalized name `diamond_powered_rail_on.png`.
- The updated copper and iron `_on` textures are distinct and are packaged as supplied.
- All six off/on textures are included in both source packs. Current item routing references the three non-`_on` textures.
