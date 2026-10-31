package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns the gun's aim into the yaw/pitch JEG's server expects. The server spawns the bullet at the player's eye, not
 * at the gun, so sending the controller direction as-is would fly parallel to the barrel and miss by the hand-to-eye
 * offset. Instead we find what the barrel points at and aim from the eye at that point.
 */
public final class VrAim {
    private static final double RANGE = 128.0;
    private static final double MIN_DISTANCE_SQR = 0.2 * 0.2;

    private VrAim() {
    }

    /** {yaw, pitch} in degrees, including the recoil deviation, or null when not aiming in VR. */
    public static float[] shotRotation(Player player) {
        VR.Aim aim = VR.mainHandAim();
        if (aim == null || aim.dir().lengthSqr() < 1.0E-6) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 toTarget = target(player, aim).subtract(eye);
        // A target almost at the eye gives a meaningless angle: keep the barrel direction then.
        Vec3 dir = (toTarget.lengthSqr() < MIN_DISTANCE_SQR ? aim.dir() : toTarget).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        return new float[]{yaw + VrRecoil.aimDrift(), Mth.clamp(pitch - VrRecoil.aimClimb(), -90.0F, 90.0F)};
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

    private static boolean canHit(Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }
}
