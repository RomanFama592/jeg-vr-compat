package com.jegvr.mixin.render;

import com.jegvr.client.VrBlindOverlay;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.RenderPass;
import org.vivecraft.client_vr.render.helpers.ShaderHelper;

/**
 * F9.27: Vivecraft's post-processing runs once per eye (LEFT and RIGHT only), after the world, the hands, the panels
 * and any shader pack, with that eye's framebuffer bound. JEG's stun and smoke overlays go there
 * ({@link VrBlindOverlay}). Only looks: if Vivecraft changes this method, the overlay stays on the HUD as before.
 */
@Mixin(value = ShaderHelper.class, remap = false)
public abstract class ShaderHelperVRMixin {
    @Inject(method = "doVrPostProcess", at = @At("TAIL"), require = 0)
    private static void jegvr$blindEye(RenderPass pass, RenderTarget source, float partialTick, CallbackInfo ci) {
        VrBlindOverlay.drawEye();
    }
}
