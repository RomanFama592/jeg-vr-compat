package com.jegvr.mixin.net;

import com.jegvr.client.VrDiag;
import com.jegvr.client.VrRecoil;
import com.jegvr.compat.VR;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.network.message.C2SMessageShoot;

/**
 * JEG's server applies the yaw/pitch carried by this message to the player before spawning the projectile. In VR we
 * send the main controller's direction instead of the head's, plus the gameplay recoil deviation, so bullets go where
 * the gun points on a server that only has JEG.
 */
@Mixin(value = C2SMessageShoot.class, remap = false)
public class C2SMessageShootVRMixin {
    @Shadow
    private float rotationYaw;
    @Shadow
    private float rotationPitch;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"))
    private void jegvr$aimFromController(Player player, CallbackInfo ci) {
        if (!player.level().isClientSide) {
            return;
        }
        Vec3 dir = VR.mainHandDirection();
        if (dir == null || dir.lengthSqr() < 1.0E-6) {
            return;
        }
        dir = dir.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        this.rotationYaw = yaw + VrRecoil.aimDrift();
        this.rotationPitch = Mth.clamp(pitch - VrRecoil.aimClimb(), -90.0F, 90.0F);
        VrDiag.once("shootDir", "VR shot aimed from controller: yaw={} pitch={} (head yaw={} pitch={})",
                this.rotationYaw, this.rotationPitch, player.getYRot(), player.getXRot());
    }
}
