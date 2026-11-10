package com.jegvr.server;

import com.jegvr.config.VrRules;
import com.jegvr.config.VrServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F9.8: a VR player's bullets may hit that same player's head or other hand (JEG always skips the shooter). In VR the
 * player's own body is not seen and Minecraft's box around it says nothing about where the head and hands are, so
 * the targets are spheres around the headset and the hand that did not fire, sent with each shot
 * ({@link com.jegvr.network.C2SVrSelfTargets}). The client only sends them while its self-damage option (or the
 * server's rule) is on; the server's {@code DENY} still refuses them. Bullets that started at the muzzle only
 * ({@code ProjectileEntityVRMixin}).
 */
public final class VrSelfDamage {
    /** A self-inflicted headshot kills: more than any armour leaves standing (a totem still saves the player). */
    static final float LETHAL_DAMAGE = 1000.0F;
    /** Head sphere: its centre this far behind the eyes, along the look direction (metres in the room). */
    static final double HEAD_BACK = 0.08;
    static final double HEAD_RADIUS = 0.13;
    /** Hand sphere, around the controller. */
    static final double HAND_RADIUS = 0.09;
    /** Targets are used for the bullets of the shot they came with: this many ticks at most. */
    static final long MAX_AGE_TICKS = 3;
    /** A target farther than this from the player's eye is ignored (same reach as the muzzle). */
    private static final double MAX_REACH = 2.5;
    private static final float MIN_SCALE = 0.05F;
    private static final float MAX_SCALE = 20.0F;

    /** Where a player's head and hands were at the last shot. A hand is null when it is not a target. */
    private record Targets(Vec3 headCentre, @Nullable Vec3 mainHand, @Nullable Vec3 offHand, double scale,
                           long gameTime) {
    }

    /** Where a bullet hit its shooter, and whether in the head. */
    public record Hit(Vec3 point, boolean head) {
    }

    private static final Map<UUID, Targets> TARGETS = new ConcurrentHashMap<>();

    private VrSelfDamage() {
    }

    public static void update(ServerPlayer player, Vec3 eyes, Vec3 look, @Nullable Vec3 mainHand,
                              @Nullable Vec3 offHand, float scale) {
        Vec3 eye = player.getEyePosition();
        if (!usable(eye, eyes) || !isFinite(look) || !Float.isFinite(scale)) {
            return;
        }
        double s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
        TARGETS.put(player.getUUID(), new Targets(headCentre(eyes, look, s),
                usable(eye, mainHand) ? mainHand : null, usable(eye, offHand) ? offHand : null, s,
                player.level().getGameTime()));
    }

    /** Whether a bullet {@code shooter} fires now from the muzzle may hit them. */
    public static boolean allows(LivingEntity shooter) {
        return VrServerConfig.selfDamageRule() != VrRules.Toggle.DENY && targets(shooter) != null;
    }

    /**
     * Where the bullet moving from {@code start} to {@code end} this tick first hits its shooter's head, or the hand
     * that did not fire it ({@code firingHand}), or null.
     */
    @Nullable
    public static Hit hit(LivingEntity shooter, InteractionHand firingHand, Vec3 start, Vec3 end) {
        Targets targets = targets(shooter);
        if (targets == null) {
            return null;
        }
        double head = entry(start, end, targets.headCentre, HEAD_RADIUS * targets.scale, true);
        Vec3 otherHand = firingHand == InteractionHand.MAIN_HAND ? targets.offHand : targets.mainHand;
        double hand = otherHand == null ? -1.0 : entry(start, end, otherHand, HAND_RADIUS * targets.scale, false);
        if (head < 0.0 && hand < 0.0) {
            return null;
        }
        boolean isHead = head >= 0.0 && (hand < 0.0 || head <= hand);
        double t = isHead ? head : hand;
        return new Hit(start.add(end.subtract(start).scale(t)), isHead);
    }

    /** The damage of a bullet that hit its own shooter: JEG's, or lethal for a headshot. */
    public static float damage(float jegDamage, boolean headshot) {
        return headshot ? Math.max(jegDamage, LETHAL_DAMAGE) : jegDamage;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TARGETS.remove(event.getEntity().getUUID());
    }

    @Nullable
    private static Targets targets(LivingEntity shooter) {
        if (!(shooter instanceof ServerPlayer player)) {
            return null;
        }
        Targets targets = TARGETS.get(player.getUUID());
        return targets != null && player.level().getGameTime() - targets.gameTime <= MAX_AGE_TICKS ? targets : null;
    }

    /** Centre of the head: behind the eyes along the look direction (the headset sits at the front of the face). */
    static Vec3 headCentre(Vec3 eyes, Vec3 look, double scale) {
        if (look.lengthSqr() < 1.0E-6) {
            return eyes;
        }
        return eyes.subtract(look.normalize().scale(HEAD_BACK * scale));
    }

    /**
     * Fraction of the segment {@code start}–{@code end} at which it enters the sphere, or -1 if it does not. A segment
     * that starts inside counts (at 0) only with {@code insideCounts} and while moving towards the centre: a gun pressed
     * to the head and pointing in, not one held beside it pointing away.
     */
    static double entry(Vec3 start, Vec3 end, Vec3 centre, double radius, boolean insideCounts) {
        Vec3 d = end.subtract(start);
        Vec3 f = start.subtract(centre);
        double a = d.dot(d);
        if (a < 1.0E-12) {
            return -1.0;
        }
        double c = f.dot(f) - radius * radius;
        double fd = f.dot(d);
        if (c <= 0.0) {
            return insideCounts && fd < 0.0 ? 0.0 : -1.0;
        }
        double disc = fd * fd - a * c;
        if (disc < 0.0) {
            return -1.0;
        }
        double t = (-fd - Math.sqrt(disc)) / a;
        return t >= 0.0 && t <= 1.0 ? t : -1.0;
    }

    private static boolean usable(Vec3 eye, @Nullable Vec3 target) {
        return target != null && isFinite(target) && target.distanceTo(eye) <= MAX_REACH;
    }

    private static boolean isFinite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
