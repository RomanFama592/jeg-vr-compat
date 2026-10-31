package com.jegvr.mixin.render;

import com.jegvr.client.VrDiag;
import com.jegvr.compat.VR;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.handler.GunRenderingHandler;

/** In VR the pose stack already sits at the controller: drop JEG's screen-space hand placement. */
@Mixin(value = GunRenderingHandler.class, remap = false)
public class GunRenderingHandlerVRMixin {
    private static final String POSE = "Lcom/mojang/blaze3d/vertex/PoseStack;";

    @Inject(method = {
            "applyBobbingTransforms", "applyAimingTransforms", "applySwayTransforms", "applySprintingTransforms",
            "applyRecoilTransforms", "applyReloadTransforms", "applyShieldTransforms", "applyMeleeTransforms"
    }, at = @At("HEAD"), cancellable = true)
    private void jegvr$skipFlatTransforms(CallbackInfo ci) {
        if (VR.active()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "onRenderOverlay",
            at = @At(value = "INVOKE", target = POSE + "translate(DDD)V", remap = true))
    private boolean jegvr$noTranslateD(PoseStack stack, double x, double y, double z) {
        return !VR.active();
    }

    @WrapWithCondition(method = "onRenderOverlay",
            at = @At(value = "INVOKE", target = POSE + "translate(FFF)V", remap = true))
    private boolean jegvr$noTranslateF(PoseStack stack, float x, float y, float z) {
        return !VR.active();
    }

    @WrapWithCondition(method = "onRenderOverlay",
            at = @At(value = "INVOKE", target = POSE + "mulPose(Lorg/joml/Quaternionf;)V", remap = true))
    private boolean jegvr$noRotate(PoseStack stack, Quaternionf rotation) {
        return !VR.active();
    }

    @Inject(method = "renderWeapon", at = @At("HEAD"))
    private void jegvr$diagRenderWeapon(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                        PoseStack poseStack, MultiBufferSource buffer, int light, float partialTick,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (VR.active() && context.firstPerson()) {
            VrDiag.once("renderWeapon", "GunRenderingHandler.renderWeapon runs in VR (context={})", context);
        }
    }
}
