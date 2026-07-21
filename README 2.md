# IronCraft Minecart Addon

A Paper 26.2 plugin that gives powered rails two target speeds without hard-capping fast incoming minecarts:

- normal gold powered rail: **0.8 blocks/tick (16 blocks/second)**;
- Copper Powered Rail: **0.4 blocks/tick (8 blocks/second)**, the previous vanilla speed;
- carts below a target use vanilla powered-rail acceleration;
- carts above a target decelerate smoothly instead of snapping to it; and
- ordinary, detector, activator, and unpowered powered rails keep their vanilla behavior and do not impose either target.

The plugin targets the current Paper 26.2 API (`26.2.build.63-beta`) and requires Java 25. Paper currently labels 26.2 builds beta/experimental, so test and back up a production world before upgrading it.

## Install

1. Run a Paper 26.2 server with Java 25.
2. Copy `IronCraftMinecartAddon-1.0.0.jar` into the server's `plugins` directory.
3. Restart the server. Do not use `/reload` for plugin installation.
4. Craft or give yourself a Copper Powered Rail.

The recipe mirrors the vanilla powered-rail recipe but replaces all six gold ingots with copper ingots. It produces six rails by default:

```text
C _ C
C S C
C R C

C = Copper Ingot
S = Stick
R = Redstone Dust
```

Use `/copperrail give [player] [amount]` for testing. `/copperrail` displays the active targets, and `/copperrail reload` reloads `config.yml`. Give/reload requires `ironcraftminecartaddon.admin` (OP by default).

## How identity and physics work

The Copper Powered Rail item is a real `minecraft:powered_rail` with custom item data. Because a placed powered rail is not a block entity and cannot contain its own NBT/PDC, placed copper coordinates are persisted in the owning chunk's PDC. This keeps the actual world block a powered rail, preserving redstone, shape, collision, minecart behavior, and vanilla-client/Geyser compatibility.

While a cart is on any rail, the plugin raises Paper's internal per-cart maximum to a high numerical safety value so a fast cart is not clamped to 0.4 or 0.8. Velocity is edited only on an active powered rail. Once the cart leaves rails, its original Paper maximum is restored.

Configuration is documented inline in `src/main/resources/config.yml`. The `physics.safety-max-speed` setting is not a rail target; it is only a guard against unreasonable numerical values.

## Geyser

Gameplay works through Geyser without a Geyser dependency or Bedrock pack. Copper items use a redundant Custom Model Data marker so current Geyser custom-item mappings can provide an optional Bedrock icon. Without that optional mapping, Bedrock players see a vanilla powered rail but still get the correct recipe, placement identity, drops, and cart speed.

See [Geyser setup](docs/GEYSER_SETUP.md) for the optional mapping and [the Blockbench/resource-pack guide](docs/RESOURCE_PACK_GUIDE.md) for Java textures and the placed-block rendering limitation.

## Build

The checked-in Gradle wrapper downloads the build tool; a Java 25 JDK must be available:

```bash
./gradlew clean build
```

The plugin JAR is written to `build/libs/IronCraftMinecartAddon-1.0.0.jar`.
