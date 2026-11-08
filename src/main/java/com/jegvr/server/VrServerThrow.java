package com.jegvr.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F9.26: a VR player's grenade leaves from the hand that throws it, towards where that hand aims (or swings), instead
 * of from the eyes along the head. JEG throws with {@code shootFromRotation(xRot, yRot, 0, 1, 1)}; with a fresh
 * {@link VrServerAim} record for the throwing hand the grenade is re-aimed before it enters the world.
 */
public final class VrServerThrow {
    /** JEG's throw speed (blocks per tick) once the grenade was held for 10 ticks. */
    public static final float JEG_SPEED = 1.0F;
    /** Limits for the speed of a thrown gesture: a soft toss up to a strong throw (a snowball is 1.5). */
    public static final float MIN_SPEED = 0.3F;
    public static final float MAX_SPEED = 1.5F;
    /** JEG's spread for throws. */
    private static final float INACCURACY = 1.0F;

    private record Speed(InteractionHand hand, float speed, long gameTime) {
    }

    private static final Map<UUID, Speed> SPEEDS = new ConcurrentHashMap<>();

    private VrServerThrow() {
    }

    public static void update(ServerPlayer player, InteractionHand hand, float speed) {
        if (!Float.isFinite(speed)) {
            return;
        }
        SPEEDS.put(player.getUUID(), new Speed(hand, clampSpeed(speed), player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SPEEDS.remove(event.getEntity().getUUID());
    }

    static float clampSpeed(float speed) {
        return Mth.clamp(speed, MIN_SPEED, MAX_SPEED);
    }

    /** Called by JEG's throw right before the grenade enters the world. */
    public static void aim(LivingEntity shooter, Entity entity) {
        if (!(entity instanceof Projectile projectile) || !shooter.isUsingItem()) {
            return;
        }
        InteractionHand hand = shooter.getUsedItemHand();
        Vec3 dir = VrServerAim.directionFor(shooter, hand);
        if (dir == null) {
            return;
        }
        Vec3 muzzle = VrServerAim.muzzleFor(shooter, hand);
        if (muzzle != null) {
            projectile.setPos(muzzle);
        }
        Speed record = SPEEDS.remove(shooter.getUUID());
        boolean gesture = record != null && record.hand == hand
                && VrServerAim.fresh(record.gameTime, shooter.level().getGameTime());
        projectile.shoot(dir.x, dir.y, dir.z, gesture ? record.speed : JEG_SPEED, INACCURACY);
        // As Projectile.shootFromRotation: the thrower's own motion carries over.
        Vec3 motion = shooter.getDeltaMovement();
        projectile.setDeltaMovement(projectile.getDeltaMovement()
                .add(motion.x, shooter.onGround() ? 0.0 : motion.y, motion.z));
    }
}
