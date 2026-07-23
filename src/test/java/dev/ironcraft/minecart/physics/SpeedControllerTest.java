package dev.ironcraft.minecart.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SpeedControllerTest {
    @Test
    void restoresVelocityDiscardedByClassicMinecartClamp() {
        double observedAfterClampAndBoost = 2.0 * 0.997 + 0.06;

        assertEquals(
                2.11382,
                SpeedController.restoreClassicClampedSpeed(
                        observedAfterClampAndBoost,
                        2.06,
                        0.06,
                        0.997
                ),
                1.0E-9
        );
    }

    @Test
    void doesNotHideARealCollisionWhileRestoringClassicSpeed() {
        assertEquals(
                0.5,
                SpeedController.restoreClassicClampedSpeed(0.5, 3.2, 0.06, 0.997),
                1.0E-9
        );
    }

    @Test
    void calculatesOnlyUntraveledOverflowDistance() {
        assertEquals(1.7, SpeedController.overflowDistance(3.2, 1.5), 1.0E-9);
        assertEquals(0.0, SpeedController.overflowDistance(1.0, 1.5), 1.0E-9);
    }

    @Test
    void leavesVanillaAccelerationUntouchedBelowTarget() {
        assertEquals(0.36, SpeedController.targetSpeed(0.36, 0.30, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void catchesNormalVanillaOvershootAtTheTarget() {
        assertEquals(0.40, SpeedController.targetSpeed(0.44, 0.38, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void deceleratesHighIncomingSpeedWithoutHardClamping() {
        assertEquals(0.94, SpeedController.targetSpeed(1.057, 1.0, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void preservesARealCollisionSlowdown() {
        assertEquals(0.50, SpeedController.targetSpeed(0.50, 1.0, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void preservesMostOfAnExternalImpulseWhileStillDecelerating() {
        assertEquals(1.88, SpeedController.targetSpeed(2.0, 1.0, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void compensatesForMultiplePoweredRailsCrossedInOneTick() {
        assertEquals(0.94, SpeedController.targetSpeed(1.117, 1.0, 0.40, 0.12, 0.06), 1.0E-9);
    }

    @Test
    void worksWithoutPreviousTickState() {
        assertEquals(0.88, SpeedController.targetSpeed(1.0, null, 0.40, 0.06, 0.06), 1.0E-9);
    }

    @Test
    void rejectsInvalidObservedSpeed() {
        assertThrows(IllegalArgumentException.class,
                () -> SpeedController.targetSpeed(Double.NaN, 0.4, 0.4, 0.06, 0.06));
    }
}
