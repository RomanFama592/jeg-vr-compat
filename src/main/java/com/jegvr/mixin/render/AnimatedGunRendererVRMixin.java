package com.jegvr.mixin.render;

import com.jegvr.client.VrDiag;
import com.jegvr.compat.VR;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import com.jegvr.client.VrHands;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ttv.migami.jeg.client.handler.ShootingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow
    private ItemDisplayContext renderType;

    // JEG draws static player arms (arm + sleeve, both hands) on the gun's arm bones; Vivecraft already draws the
    // tracked hands. Only drops the draw call: the bone transforms still apply to child bones. Only in first person:
    // the Finger Gun is drawn as those arms in the hotbar, inventory, item frames and on the ground (F9.5).
    @WrapWithCondition(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lttv/migami/jeg/item/AnimatedGunItem;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V",
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private boolean jegvr$noStaticArms(ModelPart part, PoseStack stack, VertexConsumer consumer, int light, int overlay,
                                       float r, float g, float b, float a) {
        return !VR.active() || renderType == null || !renderType.firstPerson();
    }

    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void jegvr$diagRenderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                        MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (VR.active() && context.firstPerson()) {
            VrDiag.once("renderByItem", "AnimatedGunRenderer.renderByItem runs in VR (context={})", context);
        }
    }

    /** Muzzle flash and its light boost: in VR only on the gun of the hand that fired (see {@link VrHands}). */
    @WrapOperation(method = "renderByItem", at = @At(value = "INVOKE",
            target = "Lttv/migami/jeg/client/handler/ShootingHandler;isShooting()Z"))
    private boolean jegvr$flashPerHand(ShootingHandler handler, Operation<Boolean> original,
                                       @Local(argsOnly = true) ItemDisplayContext context) {
        return VrHands.shouldFlash(original.call(handler), context);
    }
}
