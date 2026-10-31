package com.jegvr.mixin.render;

import com.jegvr.client.VrDiag;
import com.jegvr.client.VrGunPose;
import com.jegvr.compat.VR;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.render.gun.animated.AnimatedGunRenderer;

@Mixin(value = AnimatedGunRenderer.class, remap = false)
public class AnimatedGunRendererVRMixin {
    private static final String POSE = "Lcom/mojang/blaze3d/vertex/PoseStack;";

    @Inject(method = {
            "applyBobbingTransforms", "applyAimingTransforms", "applySwayTransforms", "applySprintingTransforms",
            "applyRecoilTransforms"
    }, at = @At("HEAD"), cancellable = true)
    private void jegvr$skipFlatTransforms(CallbackInfo ci) {
        if (VR.active()) {
            ci.cancel();
        }
    }

    // Ordinal 0 of translate/mulPose is the static bow orientation fix; the rest is the ADS centering.
    @WrapWithCondition(method = "renderByItem",
            at = @At(value = "INVOKE", target = POSE + "translate(DDD)V", remap = true, ordinal = 1))
    private boolean jegvr$noAdsTranslate1(PoseStack stack, double x, double y, double z) {
        return !VR.active();
    }

    @WrapWithCondition(method = "renderByItem",
            at = @At(value = "INVOKE", target = POSE + "translate(DDD)V", remap = true, ordinal = 2))
    private boolean jegvr$noAdsTranslate2(PoseStack stack, double x, double y, double z) {
        return !VR.active();
    }

    @WrapWithCondition(method = "renderByItem",
            at = @At(value = "INVOKE", target = POSE + "translate(DDD)V", remap = true, ordinal = 3))
    private boolean jegvr$noAdsTranslate3(PoseStack stack, double x, double y, double z) {
        return !VR.active();
    }

    @WrapWithCondition(method = "renderByItem",
            at = @At(value = "INVOKE", target = POSE + "mulPose(Lorg/joml/Quaternionf;)V", remap = true, ordinal = 1))
    private boolean jegvr$noAdsRotate(PoseStack stack, Quaternionf rotation) {
        return !VR.active();
    }

    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void jegvr$vrPoseRenderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                        MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (VR.active() && context.firstPerson()) {
            VrDiag.once("renderByItem", "AnimatedGunRenderer.renderByItem runs in VR (context={})", context);
            VrGunPose.apply(poseStack, stack, context);
        }
    }
}
