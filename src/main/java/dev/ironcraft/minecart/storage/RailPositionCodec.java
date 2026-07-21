package dev.ironcraft.minecart.storage;

/** Packs a chunk-local X/Z and signed absolute Y into one integer. */
public final class RailPositionCodec {
    private RailPositionCodec() {
    }

    public static int encode(int blockX, int blockY, int blockZ) {
        return (blockY << 8) | ((blockZ & 15) << 4) | (blockX & 15);
    }

    public static int localX(int packed) {
        return packed & 15;
    }

    public static int localZ(int packed) {
        return (packed >>> 4) & 15;
    }

    public static int blockY(int packed) {
        return packed >> 8;
    }
}
