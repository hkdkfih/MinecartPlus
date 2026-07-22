package dev.ironcraft.minecart.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RailSyncProtocolTest {
    @Test
    void handshakeChallengeContainsVersionAndNonce() throws Exception {
        DataInputStream input = input(RailSyncProtocol.handshakeChallenge(0x12345678));

        assertEquals(1, input.readUnsignedByte());
        assertEquals(0x12345678, input.readInt());
        assertEquals(0, input.available());
    }

    @Test
    void updateUsesExplicitTypeIdsAndZeroForRemoval() throws Exception {
        DataInputStream update = input(RailSyncProtocol.update("minecraft:overworld", 12, -4, 30, CustomRailType.DIAMOND));
        assertEquals(1, update.readUnsignedByte());
        assertEquals(3, update.readUnsignedByte());
        assertEquals("minecraft:overworld", readString(update));
        assertEquals(12, update.readInt());
        assertEquals(-4, update.readInt());
        assertEquals(30, update.readInt());
        assertEquals(3, update.readUnsignedByte());

        DataInputStream removal = input(RailSyncProtocol.update("minecraft:overworld", 12, -4, 30, null));
        removal.skipNBytes(2);
        readString(removal);
        removal.skipNBytes(12);
        assertEquals(0, removal.readUnsignedByte());
    }

    @Test
    void newRailTypesUseStableNetworkIds() throws Exception {
        assertEquals(4, updateTypeId(CustomRailType.STONE));
        assertEquals(5, updateTypeId(CustomRailType.NETHERITE));
    }

    @Test
    void largeSnapshotsAreFragmentedAndOnlyFirstFragmentReplaces() throws Exception {
        List<CustomRailRegistry.StoredRail> rails = new ArrayList<>();
        for (int index = 0; index < 4_001; index++) {
            rails.add(new CustomRailRegistry.StoredRail(index, CustomRailType.COPPER));
        }

        List<byte[]> packets = RailSyncProtocol.chunkSnapshot("minecraft:overworld", -2, 7, rails);
        assertEquals(2, packets.size());

        DataInputStream first = snapshotHeader(packets.get(0));
        assertTrue(first.readBoolean());
        assertEquals(4_000, first.readUnsignedShort());

        DataInputStream second = snapshotHeader(packets.get(1));
        assertFalse(second.readBoolean());
        assertEquals(1, second.readUnsignedShort());
    }

    private static DataInputStream snapshotHeader(byte[] packet) throws Exception {
        DataInputStream input = input(packet);
        assertEquals(1, input.readUnsignedByte());
        assertEquals(1, input.readUnsignedByte());
        assertEquals("minecraft:overworld", readString(input));
        assertEquals(-2, input.readInt());
        assertEquals(7, input.readInt());
        return input;
    }

    private static int updateTypeId(CustomRailType type) throws Exception {
        DataInputStream input = input(RailSyncProtocol.update("minecraft:overworld", 0, 0, 0, type));
        input.skipNBytes(2);
        readString(input);
        input.skipNBytes(12);
        return input.readUnsignedByte();
    }

    private static String readString(DataInputStream input) throws Exception {
        return new String(input.readNBytes(input.readUnsignedShort()), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static DataInputStream input(byte[] packet) {
        return new DataInputStream(new ByteArrayInputStream(packet));
    }
}
