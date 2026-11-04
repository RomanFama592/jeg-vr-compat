package com.jegvr.mixin.render;

import com.jegvr.client.VrGunHud;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.helpers.VRArmHelper;

/** F9.9: the panels beside the hands are drawn after the hands, so the guns cannot cover them ({@link VrGunHud}). */
@Mixin(value = VRArmHelper.class, remap = false)
public abstract class VRArmHelperVRMixin {
    @Inject(method = "renderVRHands", at = @At("RETURN"))
    private static void jegvr$panelsOverGuns(float partialTick, boolean renderMain, boolean renderOff, boolean menuHandMain,
                                             boolean menuHandOff, PoseStack poseStack, CallbackInfo ci) {
        if ((renderMain || renderOff) && !menuHandMain && !menuHandOff) {
            VrGunHud.render(poseStack);
        }
    }
}
