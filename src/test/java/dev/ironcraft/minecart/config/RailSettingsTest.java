package dev.ironcraft.minecart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.ironcraft.minecart.rail.CustomRailType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class RailSettingsTest {
    @Test
    void defaultsGiveStoneAndNetheriteRequestedSpeeds() {
        RailSettings settings = RailSettings.load(new YamlConfiguration());

        assertEquals(2.0, settings.speed(CustomRailType.STONE) * 20.0);
        assertEquals(64.0, settings.speed(CustomRailType.NETHERITE) * 20.0);
    }
}
