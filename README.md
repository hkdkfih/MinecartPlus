# MinecartPlus
> [!WARNING]
> If you stumbled upon this repo: This is a plugin for my private minecraft server. It's fully vibe coded so dont expect any code quality! I only uploaded it to GitHub because its the easiest way to host my resource pack for the server. I don't recommand you to use this but if you want, below is a pretty comprehensive README from codex.

MinecartPlus is a Paper 26.2 plugin that adds faster, vanilla-style minecart rail tiers without registering modded blocks.

| Rail | Logical ID | Actual target speed |
|---|---|---:|
| Copper Powered Rail | `ironcraft:copper_powered_rail` | 8 blocks/s |
| Normal Powered Rail | `minecraft:powered_rail` | 16 blocks/s |
| Iron Powered Rail | `ironcraft:iron_powered_rail` | 20 blocks/s |
| Diamond Powered Rail | `ironcraft:diamond_powered_rail` | 32 blocks/s |

Powered rails retain vanilla acceleration. A cart below the active rail's target accelerates as vanilla does; a faster incoming cart smoothly decelerates toward the target rather than being hard-clamped. Ordinary rails, detector rails, activator rails, and unpowered powered rails receive no custom acceleration or deceleration.

Classic minecart movement only evaluates one track segment per tick, so a fast cart can skip a curve or uphill transition. MinecartPlus looks ahead without loading chunks and brakes toward 16 blocks/s before bends and a vanilla-safe 8 blocks/s before climbs, preventing fast carts from hitting the next slope support and rolling backward. If a cart is spawned or externally accelerated immediately beside unsafe geometry, it uses an immediate safety brake. Worlds using the improved minecart experiment already iterate track segments and do not need this workaround.

The targets are player-visible traveled speeds. MinecartPlus compensates for the classic vanilla movement code's 75% occupied-cart movement multiplier, fixing the former 8 → 6 and 16 → 12 blocks/s behavior. Empty carts and worlds using the improved minecart experiment are not overcompensated.

The plugin targets Paper 26.2 (`26.2.build.63-beta`) and Java 25. Paper currently labels 26.2 builds beta/experimental, so back up a production world before upgrading it.

## Install and use

1. Run Paper 26.2 with Java 25.
2. Copy `MinecartPlus-1.1.1.jar` into `plugins/`.
3. Restart the server; do not use Bukkit `/reload` for installation.
4. Craft a tier or use the command below.

Each custom recipe copies the powered-rail recipe and substitutes the six gold ingots with that tier's material:

```text
M _ M
M S M
M R M

M = Copper Ingot, Iron Ingot, or Diamond
S = Stick
R = Redstone Dust
```

The recipe produces six rails by default.

Commands:

```text
/ironcraft
/ironcraft give @s ironcraft:copper_powered_rail
/ironcraft give @s ironcraft:iron_powered_rail 64
/ironcraft give PlayerName ironcraft:diamond_powered_rail 6
/ironcraft reload
```

`ironcraft.command` permits the information command and defaults to everyone. `ironcraft.admin` permits give/reload and defaults to operators.

### Why vanilla `/give` cannot use the logical IDs

`ironcraft:copper_powered_rail` and the other IDs are MinecartPlus logical item/recipe IDs, not entries in Minecraft's frozen item registry. A Paper plugin cannot add a true registry item while keeping unmodified Java and Geyser clients compatible. Therefore:

- `/give @s ironcraft:copper_powered_rail` does **not** work;
- `/ironcraft give @s ironcraft:copper_powered_rail` does work; and
- `/recipe give @s ironcraft:copper_powered_rail` works for the recipe ID.

JEI-like inventory viewers can show the custom recipe result, name, lore, and resource-pack model, but may group it under `minecraft:powered_rail`. Jade sees a placed rail as a powered rail because that is the real client-visible block. A dedicated integration with each mod would be needed to override those registry-based labels.

## Identity and compatibility

Every custom item is a real `minecraft:powered_rail` carrying namespaced custom data and Custom Model Data. A placed powered rail is not a block entity and cannot retain arbitrary item NBT, so MinecartPlus persists the rail type at its coordinate in the owning chunk's PDC. This preserves vanilla shape, redstone, collision, minecart tracking, and compatibility with unmodified Java clients and Geyser.

Without the optional Fabric companion, all placed tiers look like normal powered rails but behave correctly. See [Fabric client setup](docs/FABRIC_CLIENT.md) for distinct placed textures, [Geyser setup](docs/GEYSER_SETUP.md) for optional Bedrock item icons, and [the Blockbench/resource-pack guide](docs/RESOURCE_PACK_GUIDE.md) for Java item textures.

## Optional Fabric client visuals

`fabric-client/` builds a client-only Fabric 26.2 mod that renders the six sprites in `blockbench/` on placed custom rails. The real block remains a vanilla powered rail; the mod wraps its baked model and changes only the sprite at coordinates synchronized by the Paper plugin. Slopes, direction, powered state, collision, redstone, and minecart behavior stay vanilla.

The plugin sends rail data only after a nonce-based companion handshake succeeds. It starts the handshake only for clients that advertise the companion channel, retries at most three times, and never retries again during that connection after the third failure. Vanilla and Geyser clients do not advertise the channel, receive no MinecartPlus packets, and continue to see vanilla powered rails.

Install `MinecartPlus-Fabric-Client-1.1.4.jar` and Fabric API on participating Java clients. Full instructions and protocol behavior are in [docs/FABRIC_CLIENT.md](docs/FABRIC_CLIENT.md).

## Build

```bash
./gradlew clean build
./gradlew -p fabric-client clean build
```

The outputs are `build/libs/MinecartPlus-1.1.1.jar` and `fabric-client/build/libs/MinecartPlus-Fabric-Client-1.1.4.jar`.
