package com.jegvr.mixin.render;

import com.jegvr.client.VrGunHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import ttv.migami.jeg.client.render.GuiRenderer;

/** F9.9: JEG's ammo overlay and meters read as turned off while the panels float beside the hands ({@link VrGunHud}). */
@Mixin(value = GuiRenderer.class, remap = false)
public class GuiRendererVRMixin {
    @ModifyExpressionValue(method = "onRenderGameOverlay", require = 0,
            slice = @Slice(from = @At(value = "FIELD",
                    target = "Lttv/migami/jeg/Config$Display;showAmmoGUI:Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;")),
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private static Object jegvr$hideAmmoOverlay(Object showAmmo) {
        return VrGunHud.active() ? Boolean.FALSE : showAmmo;
    }

    @ModifyExpressionValue(method = "onRenderGameOverlay", require = 0,
            slice = @Slice(from = @At(value = "FIELD",
                    target = "Lttv/migami/jeg/Config$Display;showTimersGUI:Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;")),
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private static Object jegvr$hideMeters(Object showMeters) {
        return VrGunHud.active() ? Boolean.FALSE : showMeters;
    }
}
