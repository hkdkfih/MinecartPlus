package dev.ironcraft.minecart.config;

import dev.ironcraft.minecart.rail.CustomRailType;
import org.bukkit.configuration.file.FileConfiguration;

public record RailSettings(
        double poweredRailSpeed,
        double copperPoweredRailSpeed,
        double ironPoweredRailSpeed,
        double diamondPoweredRailSpeed,
        double vanillaBoostPerTick,
        double decelerationPerTick,
        double curveSpeed,
        int curveLookAhead,
        double safetyMaxSpeed,
        int recipeResultAmount,
        float copperCustomModelData,
        float ironCustomModelData,
        float diamondCustomModelData
) {
    public static RailSettings load(FileConfiguration config) {
        double poweredSpeed = positiveFinite(config.getDouble("speeds.powered-rail", 0.8), "speeds.powered-rail");
        double copperSpeed = positiveFinite(config.getDouble("speeds.copper-powered-rail", 0.4), "speeds.copper-powered-rail");
        double ironSpeed = positiveFinite(config.getDouble("speeds.iron-powered-rail", 1.0), "speeds.iron-powered-rail");
        double diamondSpeed = positiveFinite(config.getDouble("speeds.diamond-powered-rail", 1.6), "speeds.diamond-powered-rail");
        double vanillaBoost = positiveFinite(config.getDouble("physics.vanilla-boost-per-tick", 0.06), "physics.vanilla-boost-per-tick");
        double deceleration = positiveFinite(config.getDouble("physics.deceleration-per-tick", 0.06), "physics.deceleration-per-tick");
        double curveSpeed = positiveFinite(config.getDouble("physics.curve-speed", 0.8), "physics.curve-speed");
        int curveLookAhead = config.getInt("physics.curve-look-ahead", 32);
        if (curveLookAhead < 1 || curveLookAhead > 128) {
            throw new IllegalArgumentException("physics.curve-look-ahead must be between 1 and 128");
        }
        double safetyMaximum = positiveFinite(config.getDouble("physics.safety-max-speed", 64.0), "physics.safety-max-speed");

        double fastestTarget = Math.max(Math.max(poweredSpeed, copperSpeed), Math.max(ironSpeed, diamondSpeed));
        if (safetyMaximum < fastestTarget / 0.75) {
            throw new IllegalArgumentException("physics.safety-max-speed must allow the fastest occupied-cart target");
        }

        int resultAmount = config.getInt("crafting.result-amount", 6);
        if (resultAmount < 1 || resultAmount > 64) {
            throw new IllegalArgumentException("crafting.result-amount must be between 1 and 64");
        }

        float copperModelData = modelData(config, "item.copper-custom-model-data", "item.custom-model-data", 2602001.0);
        float ironModelData = modelData(config, "item.iron-custom-model-data", null, 2602002.0);
        float diamondModelData = modelData(config, "item.diamond-custom-model-data", null, 2602003.0);

        return new RailSettings(
                poweredSpeed,
                copperSpeed,
                ironSpeed,
                diamondSpeed,
                vanillaBoost,
                deceleration,
                curveSpeed,
                curveLookAhead,
                safetyMaximum,
                resultAmount,
                copperModelData,
                ironModelData,
                diamondModelData
        );
    }

    public double speed(CustomRailType type) {
        return switch (type) {
            case COPPER -> copperPoweredRailSpeed;
            case IRON -> ironPoweredRailSpeed;
            case DIAMOND -> diamondPoweredRailSpeed;
        };
    }

    public float customModelData(CustomRailType type) {
        return switch (type) {
            case COPPER -> copperCustomModelData;
            case IRON -> ironCustomModelData;
            case DIAMOND -> diamondCustomModelData;
        };
    }

    private static float modelData(
            FileConfiguration config,
            String path,
            String legacyPath,
            double fallback
    ) {
        double value = config.contains(path)
                ? config.getDouble(path)
                : legacyPath != null ? config.getDouble(legacyPath, fallback) : fallback;
        if (!Double.isFinite(value) || value < 0 || value > Float.MAX_VALUE) {
            throw new IllegalArgumentException(path + " must be a finite, non-negative float");
        }
        return (float) value;
    }

    private static double positiveFinite(double value, String path) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(path + " must be a finite number greater than zero");
        }
        return value;
    }
}
