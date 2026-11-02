package com.jegvr.mixin.model;

import com.jegvr.compat.VR;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.render.pose.BazookaPose;
import ttv.migami.jeg.client.render.pose.MiniGunPose;
import ttv.migami.jeg.client.render.pose.TwoHandedPose;
import ttv.migami.jeg.client.render.pose.WeaponPose;

/**
 * Third person (other players, Vivecraft's camera): JEG poses the arms of a player holding a gun from {@code
 * PlayerModel.setupAnim} (its {@code PlayerModelMixin}, which runs after Vivecraft's own arm animation), turns the body
 * before rendering and moves the held gun to match that pose. A VR player's arms already follow the controllers, so for
 * them none of that applies: the arms stay where Vivecraft put them and the gun stays in the hand.
 */
@Mixin(value = {WeaponPose.class, TwoHandedPose.class, MiniGunPose.class, BazookaPose.class}, remap = false)
public abstract class WeaponPoseVRMixin {
    @Inject(method = "applyPlayerModelRotation", at = @At("HEAD"), cancellable = true)
    private void jegvr$keepVrArms(Player player, ModelPart rightArm, ModelPart leftArm, ModelPart head,
                                  InteractionHand hand, float aimProgress, CallbackInfo ci) {
        if (VR.isVRPlayer(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyPlayerPreRender", at = @At("HEAD"), cancellable = true)
    private void jegvr$keepVrBody(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack,
                                  MultiBufferSource buffer, CallbackInfo ci) {
        if (VR.isVRPlayer(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyHeldItemTransforms", at = @At("HEAD"), cancellable = true)
    private void jegvr$keepVrHeldItem(Player player, InteractionHand hand, float aimProgress, PoseStack poseStack,
                                      MultiBufferSource buffer, CallbackInfo ci) {
        if (VR.isVRPlayer(player)) {
            ci.cancel();
        }
    }
}
