package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.network.C2SVrAim;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns the gun's aim into the yaw/pitch JEG's server expects.
 * <ul>
 * <li>Server with this addon: we tell it where the muzzle is ({@link C2SVrAim}), it starts the bullet there, and the
 * rotation is simply the barrel direction.</li>
 * <li>JEG-only server: the bullet starts at the eye, so the barrel direction alone would fly parallel to the barrel and
 * miss by the hand-to-eye offset. Instead we find what the barrel points at and aim from the eye at that point.</li>
 * </ul>
 */
public final class VrAim {
    private static final double RANGE = 128.0;
    private static final double MIN_DISTANCE_SQR = 0.2 * 0.2;
    /** From the controller to roughly the end of the barrel, in blocks. */
    private static final double MUZZLE_FORWARD = 0.3;

    private VrAim() {
    }

    /**
     * {yaw, pitch} in degrees, including the recoil deviation, or null when not aiming in VR. Must be called while
     * building the shoot message: it may send the muzzle position first.
     */
    public static float[] shotRotation(Player player) {
        // A drawn JEG bow shoots from the bow along the arrow; in precision mode the gun points along the line between
        // both hands.
        VR.Aim aim = VrBows.aim();
        if (aim == null) {
            aim = VrPrecision.aim(InteractionHand.MAIN_HAND, VR.mainHandAim());
        }
        if (aim == null || aim.dir().lengthSqr() < 1.0E-6) {
            return null;
        }
        Vec3 barrel = aim.dir().normalize();
        Vec3 dir;
        if (serverHasAddon()) {
            sendMuzzle(InteractionHand.MAIN_HAND, aim, barrel);
            dir = barrel;
        } else {
            Vec3 eye = player.getEyePosition();
            Vec3 toTarget = target(player, aim).subtract(eye);
            // A target almost at the eye gives a meaningless angle: keep the barrel direction then.
            dir = toTarget.lengthSqr() < MIN_DISTANCE_SQR ? barrel : toTarget.normalize();
        }
        return rotation(dir, VrRecoil.aimDrift(), VrRecoil.aimClimb());
    }

    /**
     * Off-hand version of {@link #shotRotation}: sends the off-hand muzzle and returns the barrel {yaw, pitch}, or null
     * when not aiming in VR. Only for servers with this addon (no JEG-only fallback: JEG cannot fire the off hand).
     */
    public static float[] offHandShotRotation() {
        VR.Aim aim = VrPrecision.aim(InteractionHand.OFF_HAND, VR.offHandAim());
        if (aim == null || aim.dir().lengthSqr() < 1.0E-6) {
            return null;
        }
        Vec3 barrel = aim.dir().normalize();
        sendMuzzle(InteractionHand.OFF_HAND, aim, barrel);
        return rotation(barrel, 0.0F, 0.0F);
    }

    /** True when the server has this addon's channel (the optional server component). */
    public static boolean serverHasAddon() {
        return VrNetwork.isPresent(connection());
    }

    private static void sendMuzzle(InteractionHand hand, VR.Aim aim, Vec3 barrel) {
        Vec3 muzzle = aim.pos().add(barrel.scale(MUZZLE_FORWARD));
        VrNetwork.CHANNEL.sendToServer(new C2SVrAim(hand, muzzle, barrel));
    }

    private static float[] rotation(Vec3 dir, float drift, float climb) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        return new float[]{yaw + drift, Mth.clamp(pitch - climb, -90.0F, 90.0F)};
    }

    /** First block or entity along the barrel, or a far point if it points at nothing. */
    private static Vec3 target(Player player, VR.Aim aim) {
        Vec3 from = aim.pos();
        Vec3 to = from.add(aim.dir().normalize().scale(RANGE));
        HitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            to = block.getLocation();
        }
        AABB area = new AABB(from, to).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, from, to, area,
                VrAim::canHit);
        return entity != null ? entity.getLocation() : to;
    }

    private static Connection connection() {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener == null ? null : listener.getConnection();
    }

    private static boolean canHit(Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }
}
