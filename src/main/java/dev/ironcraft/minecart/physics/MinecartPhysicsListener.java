package dev.ironcraft.minecart.physics;

import dev.ironcraft.minecart.config.RailSettings;
import dev.ironcraft.minecart.rail.CustomRailType;
import dev.ironcraft.minecart.storage.CustomRailRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.FeatureFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.util.Vector;

public final class MinecartPhysicsListener implements Listener {
    private static final double MIN_HORIZONTAL_SPEED = 1.0E-8;

    private final CustomRailRegistry registry;
    private final Supplier<RailSettings> settings;
    private final Map<UUID, Double> originalMaxSpeeds = new HashMap<>();
    private final Map<UUID, Double> previousSpeeds = new HashMap<>();
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public MinecartPhysicsListener(CustomRailRegistry registry, Supplier<RailSettings> settings) {
        this.registry = registry;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUpdate(VehicleUpdateEvent event) {
        if (!(event.getVehicle() instanceof Minecart minecart)) {
            return;
        }

        Vector velocity = minecart.getVelocity();
        double observedSpeed = horizontalSpeed(velocity);
        Location currentLocation = minecart.getLocation();
        Location previousLocation = previousLocations.put(minecart.getUniqueId(), currentLocation.clone());
        Block rail = findRail(currentLocation);
        PoweredTraversal traversal = findPoweredTraversal(previousLocation, currentLocation);

        if (rail == null) {
            releaseMaxSpeed(minecart);
            rememberSpeed(minecart, observedSpeed);
            return;
        }
        claimMaxSpeed(minecart);

        RailSettings current = settings.get();
        boolean improvedMovement = hasImprovedMovement(minecart);
        double movementFactor = TravelSpeedCalibration.movementFactor(improvedMovement, !minecart.isEmpty());
        double traveledSpeed = observedSpeed * movementFactor;

        Double travelTarget = poweredRailTarget(traversal.lastRail(), current);
        TrackGeometryLookAhead.SpeedLimitApproach speedLimitApproach = null;
        double geometrySpeed = current.curveSpeed();
        double ascentSpeed = TrackGeometryLookAhead.speedLimit(
                TrackGeometryLookAhead.SpeedLimitKind.ASCENT,
                current.curveSpeed()
        );
        if (!improvedMovement && traveledSpeed > ascentSpeed) {
            int lookAhead = CurveSpeedPlanner.requiredLookAhead(
                    traveledSpeed,
                    ascentSpeed,
                    current.decelerationPerTick(),
                    current.curveLookAhead()
            );
            speedLimitApproach = TrackGeometryLookAhead.find(rail, velocity, lookAhead);
            if (speedLimitApproach != null) {
                geometrySpeed = TrackGeometryLookAhead.speedLimit(
                        speedLimitApproach.kind(),
                        current.curveSpeed()
                );
            }
            if (speedLimitApproach != null && traveledSpeed > geometrySpeed) {
                double geometryLimit = CurveSpeedPlanner.allowedSpeed(
                        speedLimitApproach.distance(),
                        geometrySpeed,
                        current.decelerationPerTick()
                );
                travelTarget = travelTarget == null ? geometryLimit : Math.min(travelTarget, geometryLimit);
            } else {
                speedLimitApproach = null;
            }
        }

        if (travelTarget == null || observedSpeed <= MIN_HORIZONTAL_SPEED) {
            setMaxSpeedIfChanged(minecart, current.safetyMaxSpeed());
            rememberSpeed(minecart, observedSpeed);
            return;
        }

        double velocityTarget = TravelSpeedCalibration.internalTarget(travelTarget, movementFactor);
        double adjustedSpeed = SpeedController.targetSpeed(
                observedSpeed,
                previousSpeeds.get(minecart.getUniqueId()),
                velocityTarget,
                current.vanillaBoostPerTick() * traversal.boostCount(),
                current.decelerationPerTick() / movementFactor
        );

        if (speedLimitApproach != null
                && CurveSpeedPlanner.needsEmergencyBrake(speedLimitApproach.distance(), traveledSpeed)) {
            double safeGeometryVelocity = TravelSpeedCalibration.internalTarget(geometrySpeed, movementFactor);
            adjustedSpeed = Math.min(adjustedSpeed, safeGeometryVelocity);
        }

        if (Math.abs(adjustedSpeed - observedSpeed) > MIN_HORIZONTAL_SPEED) {
            double scale = adjustedSpeed / observedSpeed;
            Vector adjusted = velocity.clone();
            adjusted.setX(adjusted.getX() * scale);
            adjusted.setZ(adjusted.getZ() * scale);
            minecart.setVelocity(adjusted);
        }
        // Holding vanilla's internal maximum at the current target prevents a
        // powered rail from adding a boost that MinecartPlus must undo every
        // tick. Incoming speed is retained while smooth deceleration is active.
        setMaxSpeedIfChanged(minecart, Math.min(
                current.safetyMaxSpeed(),
                Math.max(velocityTarget, adjustedSpeed)
        ));
        rememberSpeed(minecart, adjustedSpeed);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(VehicleCreateEvent event) {
        if (event.getVehicle() instanceof Minecart minecart) {
            initialize(minecart);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Minecart minecart) {
            previousLocations.put(minecart.getUniqueId(), event.getTo().clone());
            if (findRail(event.getTo()) != null) {
                claimMaxSpeed(minecart);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Minecart minecart) {
            forget(minecart);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Minecart minecart) {
                initialize(minecart);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Minecart minecart) {
                forget(minecart);
            }
        }
    }

    public void initializeLoadedMinecarts() {
        for (World world : Bukkit.getWorlds()) {
            for (Minecart minecart : world.getEntitiesByClass(Minecart.class)) {
                initialize(minecart);
            }
        }
    }

    public void shutdown() {
        for (World world : Bukkit.getWorlds()) {
            for (Minecart minecart : world.getEntitiesByClass(Minecart.class)) {
                releaseMaxSpeed(minecart);
            }
        }
        originalMaxSpeeds.clear();
        previousSpeeds.clear();
        previousLocations.clear();
    }

    private void initialize(Minecart minecart) {
        Location location = minecart.getLocation();
        previousLocations.put(minecart.getUniqueId(), location.clone());
        rememberSpeed(minecart, horizontalSpeed(minecart.getVelocity()));
        if (findRail(location) != null) {
            claimMaxSpeed(minecart);
        }
    }

    private void claimMaxSpeed(Minecart minecart) {
        originalMaxSpeeds.computeIfAbsent(minecart.getUniqueId(), ignored -> {
            double effectiveMaximum = minecart.getMaxSpeed();
            return minecart.isInWater() ? effectiveMaximum * 2.0 : effectiveMaximum;
        });
    }

    private void releaseMaxSpeed(Minecart minecart) {
        Double original = originalMaxSpeeds.remove(minecart.getUniqueId());
        if (original != null && Double.isFinite(original) && original >= 0.0) {
            minecart.setMaxSpeed(original);
        }
    }

    private void forget(Minecart minecart) {
        originalMaxSpeeds.remove(minecart.getUniqueId());
        previousSpeeds.remove(minecart.getUniqueId());
        previousLocations.remove(minecart.getUniqueId());
    }

    private void rememberSpeed(Minecart minecart, double speed) {
        if (Double.isFinite(speed) && speed >= 0.0) {
            previousSpeeds.put(minecart.getUniqueId(), speed);
        } else {
            previousSpeeds.remove(minecart.getUniqueId());
        }
    }

    private static double horizontalSpeed(Vector velocity) {
        return Math.hypot(velocity.getX(), velocity.getZ());
    }

    private Double poweredRailTarget(Block activePoweredRail, RailSettings current) {
        if (activePoweredRail == null) {
            return null;
        }
        CustomRailType customType = registry.railTypeAt(activePoweredRail);
        return customType == null ? current.poweredRailSpeed() : current.speed(customType);
    }

    private static boolean hasImprovedMovement(Minecart minecart) {
        return minecart.getWorld().getFeatureFlags().contains(FeatureFlag.MINECART_IMPROVEMENTS);
    }

    private static void setMaxSpeedIfChanged(Minecart minecart, double maximum) {
        if (Math.abs(minecart.getMaxSpeed() - maximum) > MIN_HORIZONTAL_SPEED) {
            minecart.setMaxSpeed(maximum);
        }
    }

    private static Block findRail(Location location) {
        return findRail(location.getWorld(), location.getX(), location.getY(), location.getZ());
    }

    private static Block findRail(World world, double x, double y, double z) {
        Block at = world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (Tag.RAILS.isTagged(at.getType())) {
            return at;
        }
        Block below = at.getRelative(0, -1, 0);
        return Tag.RAILS.isTagged(below.getType()) ? below : null;
    }

    private static PoweredTraversal findPoweredTraversal(Location from, Location to) {
        if (from == null || from.getWorld() != to.getWorld()) {
            Block active = activePoweredRail(findRail(to));
            return new PoweredTraversal(active, active == null ? 0 : 1);
        }

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.min(512, Math.max(1, (int) Math.ceil(distance * 4.0)));
        Block last = null;
        Block previousActive = null;
        int boostCount = 0;

        for (int step = 0; step <= steps; step++) {
            double progress = (double) step / steps;
            Block active = activePoweredRail(findRail(
                    to.getWorld(),
                    from.getX() + dx * progress,
                    from.getY() + dy * progress,
                    from.getZ() + dz * progress
            ));
            if (active != null) {
                last = active;
                if (previousActive == null
                        || active.getX() != previousActive.getX()
                        || active.getY() != previousActive.getY()
                        || active.getZ() != previousActive.getZ()) {
                    boostCount++;
                }
            }
            previousActive = active;
        }
        return new PoweredTraversal(last, boostCount);
    }

    private static Block activePoweredRail(Block rail) {
        if (rail == null
                || rail.getType() != Material.POWERED_RAIL
                || !(rail.getBlockData() instanceof Powerable powerable)
                || !powerable.isPowered()) {
            return null;
        }
        return rail;
    }

    private record PoweredTraversal(Block lastRail, int boostCount) {
    }
}
