package dev.ironcraft.minecart.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RailPositionCodecTest {
    @ParameterizedTest
    @CsvSource({
            "0,-64,0",
            "15,319,15",
            "-1,0,-1",
            "32,128,-33"
    })
    void roundTripsChunkLocalCoordinatesAndSignedY(int x, int y, int z) {
        int packed = RailPositionCodec.encode(x, y, z);
        assertEquals(x & 15, RailPositionCodec.localX(packed));
        assertEquals(z & 15, RailPositionCodec.localZ(packed));
        assertEquals(y, RailPositionCodec.blockY(packed));
    }
}
