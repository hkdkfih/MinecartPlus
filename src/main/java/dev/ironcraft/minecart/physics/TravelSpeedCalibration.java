package dev.ironcraft.minecart.physics;

/** Converts player-visible travel targets to vanilla's internal minecart velocity. */
public final class TravelSpeedCalibration {
    private static final double CLASSIC_OCCUPIED_MOVEMENT_FACTOR = 0.75;

    private TravelSpeedCalibration() {
    }

    public static double movementFactor(boolean improvedMinecartMovement, boolean occupied) {
        return !improvedMinecartMovement && occupied ? CLASSIC_OCCUPIED_MOVEMENT_FACTOR : 1.0;
    }

    public static double internalTarget(double traveledSpeedTarget, double movementFactor) {
        if (!Double.isFinite(traveledSpeedTarget) || traveledSpeedTarget <= 0.0) {
            throw new IllegalArgumentException("traveledSpeedTarget must be finite and positive");
        }
        if (!Double.isFinite(movementFactor) || movementFactor <= 0.0 || movementFactor > 1.0) {
            throw new IllegalArgumentException("movementFactor must be in (0, 1]");
        }
        return traveledSpeedTarget / movementFactor;
    }
}
