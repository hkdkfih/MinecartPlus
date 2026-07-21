package dev.ironcraft.minecart.physics;

/** Pure braking-distance math for safe classic-movement curves. */
public final class CurveSpeedPlanner {
    private static final double SAFETY_MARGIN_BLOCKS = 2.0;

    private CurveSpeedPlanner() {
    }

    public static double allowedSpeed(double distanceToCurve, double curveSpeed, double decelerationPerTick) {
        if (!Double.isFinite(distanceToCurve) || distanceToCurve < 0.0) {
            throw new IllegalArgumentException("distanceToCurve must be finite and non-negative");
        }
        if (!Double.isFinite(curveSpeed) || curveSpeed <= 0.0) {
            throw new IllegalArgumentException("curveSpeed must be finite and positive");
        }
        if (!Double.isFinite(decelerationPerTick) || decelerationPerTick <= 0.0) {
            throw new IllegalArgumentException("decelerationPerTick must be finite and positive");
        }

        double brakingDistance = Math.max(0.0, distanceToCurve - SAFETY_MARGIN_BLOCKS);
        return Math.sqrt(curveSpeed * curveSpeed + 2.0 * decelerationPerTick * brakingDistance);
    }

    public static boolean needsEmergencyBrake(double distanceToCurve, double traveledSpeed) {
        return distanceToCurve <= traveledSpeed + 1.0;
    }

    public static int requiredLookAhead(
            double traveledSpeed,
            double curveSpeed,
            double decelerationPerTick,
            int configuredMaximum
    ) {
        if (traveledSpeed <= curveSpeed) {
            return 0;
        }
        double brakingDistance = (traveledSpeed * traveledSpeed - curveSpeed * curveSpeed)
                / (2.0 * decelerationPerTick);
        int required = (int) Math.ceil(brakingDistance - 1.0E-9)
                + (int) SAFETY_MARGIN_BLOCKS + 1;
        return Math.min(configuredMaximum, Math.max(1, required));
    }
}
