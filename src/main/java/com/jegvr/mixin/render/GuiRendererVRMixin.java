package com.jegvr.mixin.render;

import com.jegvr.client.VrGunHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import ttv.migami.jeg.client.render.GuiRenderer;

/** F9.9: JEG's ammo overlay reads as turned off while each gun's panel floats beside its hand ({@link VrGunHud}). */
@Mixin(value = GuiRenderer.class, remap = false)
public class GuiRendererVRMixin {
    @ModifyExpressionValue(method = "onRenderGameOverlay",
            slice = @Slice(from = @At(value = "FIELD",
                    target = "Lttv/migami/jeg/Config$Display;showAmmoGUI:Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;")),
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private static Object jegvr$hideAmmoOverlay(Object showAmmo) {
        return VrGunHud.active() ? Boolean.FALSE : showAmmo;
    }
}
