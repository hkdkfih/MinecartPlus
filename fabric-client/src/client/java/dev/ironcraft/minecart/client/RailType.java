package dev.ironcraft.minecart.client;

import net.minecraft.resources.Identifier;

public enum RailType {
    COPPER("copper_powered_rail"),
    IRON("iron_powered_rail"),
    DIAMOND("diamond_powered_rail");

    private final Identifier offTexture;
    private final Identifier onTexture;

    RailType(String texturePath) {
        offTexture = Identifier.fromNamespaceAndPath("ironcraft", "block/" + texturePath);
        onTexture = Identifier.fromNamespaceAndPath("ironcraft", "block/" + texturePath + "_on");
    }

    public Identifier texture(boolean powered) {
        return powered ? onTexture : offTexture;
    }

    public static RailType fromNetworkId(int id) {
        return switch (id) {
            case 1 -> COPPER;
            case 2 -> IRON;
            case 3 -> DIAMOND;
            default -> null;
        };
    }
}
