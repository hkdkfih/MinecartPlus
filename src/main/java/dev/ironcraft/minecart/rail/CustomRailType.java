package dev.ironcraft.minecart.rail;

import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public enum CustomRailType {
    STONE("stone_powered_rail", "Stone Powered Rail", Material.STONE, 0x9E9E9E),
    COPPER("copper_powered_rail", "Copper Powered Rail", Material.COPPER_INGOT, 0xC15A36),
    IRON("iron_powered_rail", "Iron Powered Rail", Material.IRON_INGOT, 0xD8D8D8),
    DIAMOND("diamond_powered_rail", "Diamond Powered Rail", Material.DIAMOND, 0x55FFFF),
    NETHERITE("netherite_powered_rail", "Netherite Powered Rail", Material.NETHERITE_INGOT, 0x4A3438);

    private final String path;
    private final String displayName;
    private final Material recipeMaterial;
    private final int textColor;

    CustomRailType(String path, String displayName, Material recipeMaterial, int textColor) {
        this.path = path;
        this.displayName = displayName;
        this.recipeMaterial = recipeMaterial;
        this.textColor = textColor;
    }

    public String path() {
        return path;
    }

    public String logicalId() {
        return "ironcraft:" + path;
    }

    public NamespacedKey key() {
        return new NamespacedKey("ironcraft", path);
    }

    public String displayName() {
        return displayName;
    }

    public Material recipeMaterial() {
        return recipeMaterial;
    }

    public int textColor() {
        return textColor;
    }

    public static CustomRailType fromLogicalId(String logicalId) {
        return Arrays.stream(values())
                .filter(type -> type.logicalId().equals(logicalId))
                .findFirst()
                .orElse(null);
    }
}
