package com.jegvr.mixin.model;

import com.jegvr.compat.VR;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.render.pose.OneHandedPose;

/** One-handed guns: same as {@link WeaponPoseVRMixin}, a VR player's arm keeps Vivecraft's pose. */
@Mixin(value = OneHandedPose.class, remap = false)
public abstract class OneHandedPoseVRMixin {
    @Inject(method = "applyPlayerModelRotation", at = @At("HEAD"), cancellable = true)
    private void jegvr$keepVrArms(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                                  InteractionHand hand, float aimProgress, CallbackInfo ci) {
        if (VR.isVRPlayer(player)) {
            ci.cancel();
        }
    }
}
