package dev.ironcraft.minecart.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CurveSpeedPlannerTest {
    @Test
    void usesVanillaSpeedAtAndImmediatelyBeforeCurve() {
        assertEquals(0.4, CurveSpeedPlanner.allowedSpeed(0.0, 0.4, 0.06), 1.0E-12);
        assertEquals(0.4, CurveSpeedPlanner.allowedSpeed(2.0, 0.4, 0.06), 1.0E-12);
    }

    @Test
    void permitsMoreSpeedWithEnoughBrakingDistance() {
        assertEquals(Math.sqrt(0.4 * 0.4 + 2.0 * 0.06 * 18.0),
                CurveSpeedPlanner.allowedSpeed(20.0, 0.4, 0.06), 1.0E-12);
    }

    @Test
    void identifiesTooCloseForSmoothBraking() {
        assertTrue(CurveSpeedPlanner.needsEmergencyBrake(2.0, 1.6));
        assertFalse(CurveSpeedPlanner.needsEmergencyBrake(8.0, 1.6));
    }

    @Test
    void scansOnlyAsFarAsCurrentSpeedRequires() {
        assertEquals(0, CurveSpeedPlanner.requiredLookAhead(0.4, 0.4, 0.06, 32));
        assertEquals(23, CurveSpeedPlanner.requiredLookAhead(1.6, 0.4, 0.06, 32));
        assertEquals(7, CurveSpeedPlanner.requiredLookAhead(0.8, 0.4, 0.06, 32));
        assertEquals(10, CurveSpeedPlanner.requiredLookAhead(1.6, 0.4, 0.06, 10));
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> CurveSpeedPlanner.allowedSpeed(-1.0, 0.4, 0.06));
    }
}
