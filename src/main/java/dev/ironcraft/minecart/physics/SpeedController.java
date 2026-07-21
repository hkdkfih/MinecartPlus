package dev.ironcraft.minecart.physics;

/** Pure target-speed math, separated so it can be tested without a server. */
public final class SpeedController {
    private SpeedController() {
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
