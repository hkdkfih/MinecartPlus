package dev.ironcraft.minecart.physics;

/** Pure target-speed math, separated so it can be tested without a server. */
public final class SpeedController {
    public static final double CLASSIC_INTERNAL_SPEED_LIMIT = 2.0;
    private static final double CLASSIC_CLAMP_TOLERANCE = 0.03;

    private SpeedController() {
    }

    /**
     * Reconstructs the velocity that classic minecart movement discarded at its
     * hard-coded 2 blocks/tick rail clamp. A collision or any other velocity
     * outside the narrow post-clamp window remains authoritative.
     */
    public static double restoreClassicClampedSpeed(
            double observedSpeed,
            Double previousSpeed,
            double vanillaBoost,
            double slowdownFactor
    ) {
        if (previousSpeed == null || previousSpeed <= CLASSIC_INTERNAL_SPEED_LIMIT) {
            return observedSpeed;
        }
        double clampedPostTickSpeed = CLASSIC_INTERNAL_SPEED_LIMIT * slowdownFactor + vanillaBoost;
        if (Math.abs(observedSpeed - clampedPostTickSpeed) > CLASSIC_CLAMP_TOLERANCE) {
            return observedSpeed;
        }
        return Math.max(observedSpeed, previousSpeed * slowdownFactor + vanillaBoost);
    }

    public static double overflowDistance(double desiredTravelDistance, double observedTravelDistance) {
        if (!Double.isFinite(desiredTravelDistance) || desiredTravelDistance < 0.0) {
            throw new IllegalArgumentException("desiredTravelDistance must be finite and non-negative");
        }
        if (!Double.isFinite(observedTravelDistance) || observedTravelDistance < 0.0) {
            throw new IllegalArgumentException("observedTravelDistance must be finite and non-negative");
        }
        return Math.max(0.0, desiredTravelDistance - observedTravelDistance);
    }

    /**
     * Leaves vanilla acceleration untouched below the target. Above the target,
     * removes the vanilla boost and then decelerates smoothly. The previous
     * post-plugin speed lets an incoming cart keep speed instead of being hard
     * clamped, while {@code observedSpeed} still wins when a collision slowed it.
     */
    public static double targetSpeed(
            double observedSpeed,
            Double previousSpeed,
            double targetSpeed,
            double vanillaBoost,
            double deceleration
    ) {
        if (!Double.isFinite(observedSpeed) || observedSpeed < 0.0) {
            throw new IllegalArgumentException("observedSpeed must be finite and non-negative");
        }
        if (observedSpeed <= targetSpeed) {
            return observedSpeed;
        }

        double speedBeforeVanillaBoost = Math.max(0.0, observedSpeed - vanillaBoost);
        double effectiveIncoming = previousSpeed == null
                ? speedBeforeVanillaBoost
                : Math.max(previousSpeed, speedBeforeVanillaBoost);
        double decelerated = effectiveIncoming - deceleration;

        return Math.max(targetSpeed, Math.min(observedSpeed, decelerated));
    }
}
