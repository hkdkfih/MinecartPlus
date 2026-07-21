package dev.ironcraft.minecart.item;

import dev.ironcraft.minecart.config.RailSettings;
import dev.ironcraft.minecart.rail.CustomRailType;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

public final class CustomRailItemFactory {
    private static final NamespacedKey RAIL_ID_KEY = new NamespacedKey("ironcraft", "rail_id");
    private final Map<CustomRailType, NamespacedKey> markerKeys = new EnumMap<>(CustomRailType.class);
    private final Supplier<RailSettings> settings;

    public CustomRailItemFactory(Supplier<RailSettings> settings) {
        this.settings = settings;
        for (CustomRailType type : CustomRailType.values()) {
            markerKeys.put(type, type.key());
        }
    }

    public ItemStack create(CustomRailType type, int amount) {
        ItemStack item = new ItemStack(Material.POWERED_RAIL, amount);
        ItemMeta meta = item.getItemMeta();
        RailSettings current = settings.get();

        meta.displayName(Component.text(type.displayName(), TextColor.color(type.textColor()))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(
                                "Target speed: " + blocksPerSecond(current.speed(type)) + " blocks/second",
                                NamedTextColor.GRAY
                        )
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("ID: " + type.logicalId(), NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(markerKeys.get(type), PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(RAIL_ID_KEY, PersistentDataType.STRING, type.logicalId());

        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        modelData.setFloats(List.of(current.customModelData(type)));
        modelData.setStrings(List.of(type.logicalId()));
        meta.setCustomModelDataComponent(modelData);

        item.setItemMeta(meta);
        return item;
    }

    public CustomRailType typeOf(ItemStack item) {
        if (item == null || item.getType() != Material.POWERED_RAIL || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        String storedId = meta.getPersistentDataContainer().get(RAIL_ID_KEY, PersistentDataType.STRING);
        CustomRailType storedType = CustomRailType.fromLogicalId(storedId);
        if (storedType != null) {
            return storedType;
        }

        for (CustomRailType type : CustomRailType.values()) {
            if (meta.getPersistentDataContainer().getOrDefault(
                    markerKeys.get(type),
                    PersistentDataType.BOOLEAN,
                    false
            )) {
                return type;
            }
        }

        if (!meta.hasCustomModelDataComponent()) {
            return null;
        }
        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        if (!modelData.getStrings().isEmpty()) {
            CustomRailType stringType = CustomRailType.fromLogicalId(modelData.getStrings().getFirst());
            if (stringType != null) {
                return stringType;
            }
        }
        if (!modelData.getFloats().isEmpty()) {
            float value = modelData.getFloats().getFirst();
            for (CustomRailType type : CustomRailType.values()) {
                if (Float.compare(value, settings.get().customModelData(type)) == 0) {
                    return type;
                }
            }
        }
        return null;
    }

    public boolean isCustomRail(ItemStack item) {
        return typeOf(item) != null;
    }

    private static String blocksPerSecond(double blocksPerTick) {
        return String.format(Locale.ROOT, "%.1f", blocksPerTick * 20.0);
    }
}
