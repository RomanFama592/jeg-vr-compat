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

    private VrServerAim() {
    }

    public static void update(ServerPlayer player, InteractionHand hand, Vec3 muzzle, Vec3 direction) {
        // Only the main hand fires in JEG; the off hand is reserved for dual wield (F9.6).
        if (hand != InteractionHand.MAIN_HAND || !isFinite(muzzle) || !isFinite(direction)) {
            return;
        }
        MAIN_HAND.put(player.getUUID(), new Aim(muzzle, direction, player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MAIN_HAND.remove(event.getEntity().getUUID());
    }

    /** Validated muzzle position for a projectile fired now by {@code shooter}, or null to keep JEG's origin. */
    public static Vec3 muzzleFor(LivingEntity shooter) {
        if (!(shooter instanceof ServerPlayer player)) {
            return null;
        }
        Aim aim = MAIN_HAND.get(player.getUUID());
        if (aim == null || player.level().getGameTime() - aim.gameTime > MAX_AGE_TICKS) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        if (aim.muzzle.distanceTo(eye) > MAX_REACH) {
            return null;
        }
        // Never let the muzzle reach through a wall the player's head is not through.
        BlockHitResult wall = player.level().clip(new ClipContext(eye, aim.muzzle, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            Vec3 back = eye.subtract(aim.muzzle).normalize().scale(WALL_MARGIN);
            return wall.getLocation().add(back);
        }
        return aim.muzzle;
    }

    private static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
