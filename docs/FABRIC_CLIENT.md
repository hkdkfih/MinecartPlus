# MinecartPlus Fabric Client

The optional companion is a client-only Fabric mod for Minecraft 26.2. It gives placed copper, iron, and diamond powered rails distinct off/on textures without registering new blocks or changing gameplay.

## Install

Server:

1. Run Paper 26.2 and install `MinecartPlus-1.1.1.jar` in `plugins/`.
2. Restart the server. No Fabric server loader or proxy mod is required.

Participating Java clients:

1. Install Fabric Loader 0.19.3 or newer for Minecraft 26.2.
2. Install Fabric API 0.155.2+26.2 or newer.
3. Put `MinecartPlus-Fabric-Client-1.1.4.jar` in the client `mods/` directory.

Clients without the mod can join normally. Geyser players are also unaffected.

## Safety and compatibility

The client advertises `ironcraft:rail_handshake` through Fabric's normal custom-payload registration. The Paper plugin does not send a challenge unless that channel was advertised. A challenge contains protocol version 1 and a random per-session nonce; the client must return the same nonce on `ironcraft:rail_hello`.

Only after that response is validated does the server send `ironcraft:rail_sync` data. A client that advertises the channel but does not answer is tried at most three times, 40 ticks apart. After the third attempt, the plugin does not try again until that player disconnects and starts a new session.

Rail state is sent as authoritative per-chunk snapshots, incremental placement/removal updates, and chunk-forget messages. Snapshots are fragmented at 4,000 entries to keep packet sizes bounded. The client clears its cache on disconnect and invalidates only affected render sections.

The renderer wraps the vanilla powered-rail baked model through Fabric Renderer API. It preserves the vanilla rail's shape and powered state, then remaps its quad UVs to the matching sprite from `blockbench/`. It does not replace the block, collision, redstone behavior, or minecart logic.

## Build

From the repository root, with Java 25 available:

```bash
./gradlew -p fabric-client clean build
```

The client JAR is written to:

```text
fabric-client/build/libs/MinecartPlus-Fabric-Client-1.1.4.jar
```

The build embeds all six PNGs directly from `blockbench/`; `diamond_rail_on.png` is normalized to `diamond_powered_rail_on.png` inside the JAR.
