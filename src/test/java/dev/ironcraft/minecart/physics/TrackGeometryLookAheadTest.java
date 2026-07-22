package dev.ironcraft.minecart.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.block.data.Rail;
import org.junit.jupiter.api.Test;

class TrackGeometryLookAheadTest {
    @Test
    void limitsEveryCurveFromEitherDirection() {
        Rail.Shape[] curves = {
                Rail.Shape.NORTH_EAST,
                Rail.Shape.NORTH_WEST,
                Rail.Shape.SOUTH_EAST,
                Rail.Shape.SOUTH_WEST
        };

        for (Rail.Shape curve : curves) {
            assertEquals(TrackGeometryLookAhead.SpeedLimitKind.CURVE,
                    TrackGeometryLookAhead.speedLimitKind(curve, 1, 0));
            assertEquals(TrackGeometryLookAhead.SpeedLimitKind.CURVE,
                    TrackGeometryLookAhead.speedLimitKind(curve, -1, 0));
            assertEquals(TrackGeometryLookAhead.SpeedLimitKind.CURVE,
                    TrackGeometryLookAhead.speedLimitKind(curve, 0, 1));
            assertEquals(TrackGeometryLookAhead.SpeedLimitKind.CURVE,
                    TrackGeometryLookAhead.speedLimitKind(curve, 0, -1));
        }
    }

    @Test
    void limitsAllFourUphillDirections() {
        assertEquals(TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_EAST, 1, 0));
        assertEquals(TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_WEST, -1, 0));
        assertEquals(TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_NORTH, 0, -1));
        assertEquals(TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_SOUTH, 0, 1));
    }

    @Test
    void leavesDescendingSlopesUnrestricted() {
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_EAST, -1, 0));
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_WEST, 1, 0));
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_NORTH, 0, 1));
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.ASCENDING_SOUTH, 0, -1));
    }

    @Test
    void leavesStraightLevelTrackUnrestricted() {
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.EAST_WEST, 1, 0));
        assertNull(TrackGeometryLookAhead.speedLimitKind(Rail.Shape.NORTH_SOUTH, 0, 1));
    }

    @Test
    void keepsClassicAscentsBelowOneBlockPerTick() {
        TrackGeometryLookAhead.SpeedLimitKind kind = TrackGeometryLookAhead.speedLimitKind(
                Rail.Shape.ASCENDING_EAST,
                1,
                0
        );
        double ascentLimit = TrackGeometryLookAhead.speedLimit(kind, 1.6);

        assertEquals(0.4, ascentLimit);
        assertEquals(0.4, CurveSpeedPlanner.allowedSpeed(0.0, ascentLimit, 0.06));
        assertEquals(23, CurveSpeedPlanner.requiredLookAhead(1.6, ascentLimit, 0.06, 32));
        assertEquals(0.4, TrackGeometryLookAhead.speedLimit(
                TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                0.4
        ));
        assertEquals(0.2, TrackGeometryLookAhead.speedLimit(
                TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                0.2
        ));
        assertEquals(1.6, TrackGeometryLookAhead.speedLimit(
                TrackGeometryLookAhead.SpeedLimitKind.CURVE,
                1.6
        ));
    }
}
