package com.jegvr.mixin.model;

import com.jegvr.compat.VR;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.render.pose.WeaponPose;

/**
 * Third person: JEG tilts a held gun by the head's pitch ({@code applyHeldItemTransforms} blends its up, forward and
 * down item poses with {@code getPlayerPitch}). For a VR player the head is the headset, so the gun leaned towards
 * where they looked instead of where the controller points; seen from Vivecraft's camera it pointed off the hand. Their
 * gun already follows the controller through the arm, so it keeps the forward pose.
 */
@Mixin(value = WeaponPose.class, remap = false)
public abstract class WeaponPosePitchVRMixin {
    @Inject(method = "getPlayerPitch", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHeadPitch(Player player, CallbackInfoReturnable<Float> cir) {
        if (VR.isVRPlayer(player)) {
            cir.setReturnValue(0.0F);
        }
    }
}
