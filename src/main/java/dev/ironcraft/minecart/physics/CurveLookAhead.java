package dev.ironcraft.minecart.physics;

import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Rail;
import org.bukkit.util.Vector;

/** Lightweight forward scan; it never loads chunks. */
final class CurveLookAhead {
    private CurveLookAhead() {
    }

    static CurveApproach find(Block startingRail, Vector velocity, int maximumDistance) {
        if (startingRail == null || horizontalSpeed(velocity) < 1.0E-8) {
            return null;
        }

        int directionX = Math.abs(velocity.getX()) >= Math.abs(velocity.getZ())
                ? sign(velocity.getX()) : 0;
        int directionZ = directionX == 0 ? sign(velocity.getZ()) : 0;
        Block rail = startingRail;

        for (int distance = 0; distance <= maximumDistance; distance++) {
            if (!(rail.getBlockData() instanceof Rail data)) {
                return null;
            }
            Rail.Shape shape = data.getShape();
            if (isCurve(shape)) {
                return new CurveApproach(distance);
            }

            if (runsEastWest(shape)) {
                if (directionX == 0) {
                    directionX = sign(velocity.getX());
                    directionZ = 0;
                }
            } else if (runsNorthSouth(shape)) {
                if (directionZ == 0) {
                    directionX = 0;
                    directionZ = sign(velocity.getZ());
                }
            } else {
                return null;
            }

            if (distance == maximumDistance || (directionX == 0 && directionZ == 0)) {
                return null;
            }
            rail = nextRail(rail, directionX, directionZ);
            if (rail == null) {
                return null;
            }
        }
        return null;
    }

    private static Block nextRail(Block current, int directionX, int directionZ) {
        World world = current.getWorld();
        int x = current.getX() + directionX;
        int z = current.getZ() + directionZ;
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }

        int y = current.getY();
        Block sameLevel = world.getBlockAt(x, y, z);
        if (Tag.RAILS.isTagged(sameLevel.getType())) {
            return sameLevel;
        }
        Block above = world.getBlockAt(x, y + 1, z);
        if (Tag.RAILS.isTagged(above.getType())) {
            return above;
        }
        Block below = world.getBlockAt(x, y - 1, z);
        return Tag.RAILS.isTagged(below.getType()) ? below : null;
    }

    private static boolean isCurve(Rail.Shape shape) {
        return switch (shape) {
            case NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST -> true;
            default -> false;
        };
    }

    private static boolean runsEastWest(Rail.Shape shape) {
        return switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> true;
            default -> false;
        };
    }

    private static boolean runsNorthSouth(Rail.Shape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> true;
            default -> false;
        };
    }

    private static int sign(double value) {
        return value > 0.0 ? 1 : value < 0.0 ? -1 : 0;
    }

    private static double horizontalSpeed(Vector velocity) {
        return Math.hypot(velocity.getX(), velocity.getZ());
    }

    record CurveApproach(int distance) {
    }
}
