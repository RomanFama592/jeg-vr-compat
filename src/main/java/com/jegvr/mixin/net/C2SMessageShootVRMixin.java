package com.jegvr.mixin.net;

import com.jegvr.client.VrAim;
import com.jegvr.client.VrDiag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.network.message.C2SMessageShoot;

/**
 * JEG's server applies the yaw/pitch carried by this message to the player before spawning the projectile. In VR we
 * send the rotation that makes the bullet hit what the gun points at (see {@link VrAim}), plus the gameplay recoil
 * deviation, so it works on a server that only has JEG.
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
        float[] rotation = VrAim.shotRotation(player);
        if (rotation == null) {
            return;
        }
        this.rotationYaw = rotation[0];
        this.rotationPitch = rotation[1];
        VrDiag.once("shootDir", "VR shot aimed from controller: yaw={} pitch={} (head yaw={} pitch={})",
                this.rotationYaw, this.rotationPitch, player.getYRot(), player.getXRot());
    }
}
