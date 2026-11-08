package com.jegvr.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side record of where each VR player's gun is, fed by {@link com.jegvr.network.C2SVrAim}. JEG spawns bullets
 * at the eye; with a fresh record we move them to the muzzle instead.
 */
public final class VrServerAim {
    /** A muzzle further than this from the eye is ignored (anti-cheat; generous for large world scales). */
    private static final double MAX_REACH = 2.5;
    /** The record is only used for shots processed in the same or the next tick. */
    private static final long MAX_AGE_TICKS = 1;
    /** Pulls the origin back from a wall between the eye and the muzzle, so the bullet hits that wall. */
    private static final double WALL_MARGIN = 0.05;

    private record Aim(Vec3 muzzle, Vec3 direction, long gameTime) {
    }

    private static final Map<UUID, Aim> MAIN_HAND = new ConcurrentHashMap<>();
    /** Off-hand gun, fired through {@link com.jegvr.network.C2SVrShootOffhand} (dual wield). */
    private static final Map<UUID, Aim> OFF_HAND = new ConcurrentHashMap<>();

    private VrServerAim() {
    }

    public static void update(ServerPlayer player, InteractionHand hand, Vec3 muzzle, Vec3 direction) {
        if (!isFinite(muzzle) || !isFinite(direction)) {
            return;
        }
        map(hand).put(player.getUUID(), new Aim(muzzle, direction, player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MAIN_HAND.remove(event.getEntity().getUUID());
        OFF_HAND.remove(event.getEntity().getUUID());
    }

    /**
     * Validated muzzle position for a projectile fired now by {@code shooter} from {@code hand}, or null to keep JEG's
     * origin.
     */
    public static Vec3 muzzleFor(LivingEntity shooter, InteractionHand hand) {
        if (!(shooter instanceof ServerPlayer player)) {
            return null;
        }
        Aim aim = map(hand).get(player.getUUID());
        if (aim == null || !fresh(aim.gameTime, player.level().getGameTime())) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        if (!inReach(eye, aim.muzzle)) {
            return null;
        }
        // Never let the muzzle reach through a wall the player's head is not through.
        BlockHitResult wall = player.level().clip(new ClipContext(eye, aim.muzzle, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return wall.getType() != HitResult.Type.MISS ? beforeWall(eye, aim.muzzle, wall.getLocation()) : aim.muzzle;
    }

    /** A record is used only in the tick it arrived or the next one. */
    static boolean fresh(long recorded, long now) {
        return now - recorded <= MAX_AGE_TICKS;
    }

    /** A muzzle farther than {@link #MAX_REACH} from the eye is not believed (anti-cheat). */
    static boolean inReach(Vec3 eye, Vec3 muzzle) {
        return muzzle.distanceTo(eye) <= MAX_REACH;
    }

    /** The origin for a muzzle behind a wall: where the eye-to-muzzle line meets the wall, pulled back towards the eye. */
    static Vec3 beforeWall(Vec3 eye, Vec3 muzzle, Vec3 wallHit) {
        return wallHit.add(eye.subtract(muzzle).normalize().scale(WALL_MARGIN));
    }

    /** Unit vector along the barrel of the gun in {@code hand} from a fresh record, or null without one. */
    public static Vec3 directionFor(LivingEntity shooter, InteractionHand hand) {
        Aim aim = map(hand).get(shooter.getUUID());
        if (aim == null || !fresh(aim.gameTime, shooter.level().getGameTime())
                || aim.direction.lengthSqr() < 1.0E-6) {
            return null;
        }
        return aim.direction.normalize();
    }

    private static Map<UUID, Aim> map(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? OFF_HAND : MAIN_HAND;
    }

    static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
