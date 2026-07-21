package dev.ironcraft.minecart.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TravelSpeedCalibrationTest {
    @Test
    void classicOccupiedCartActuallyTravelsAtConfiguredTarget() {
        double movementFactor = TravelSpeedCalibration.movementFactor(false, true);

        assertEquals(0.75, movementFactor, 1.0E-12);
        assertEquals(0.4, TravelSpeedCalibration.internalTarget(0.4, movementFactor) * movementFactor, 1.0E-12);
        assertEquals(0.8, TravelSpeedCalibration.internalTarget(0.8, movementFactor) * movementFactor, 1.0E-12);
        assertEquals(1.0, TravelSpeedCalibration.internalTarget(1.0, movementFactor) * movementFactor, 1.0E-12);
        assertEquals(1.6, TravelSpeedCalibration.internalTarget(1.6, movementFactor) * movementFactor, 1.0E-12);
    }

    @Test
    void emptyOrImprovedMinecartsNeedNoCompensation() {
        assertEquals(1.0, TravelSpeedCalibration.movementFactor(false, false), 1.0E-12);
        assertEquals(1.0, TravelSpeedCalibration.movementFactor(true, true), 1.0E-12);
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> TravelSpeedCalibration.internalTarget(0.4, 0.0));
    }
}
