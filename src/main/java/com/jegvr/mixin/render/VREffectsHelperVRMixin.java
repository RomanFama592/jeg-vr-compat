package com.jegvr.mixin.render;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;
import ttv.migami.jeg.item.GunItem;

/** Hides Vivecraft's in-world crosshair while a JEG gun is in either hand: the gun itself is the sight. */
@Mixin(value = VREffectsHelper.class, remap = false)
public class VREffectsHelperVRMixin {
    @Inject(method = "shouldRenderCrosshair", at = @At("RETURN"), cancellable = true, require = 0)
    private static void jegvr$noCrosshairWithGun(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        // This class only renders in VR, so no VR.active() check is needed.
        if (cir.getReturnValueZ() && player != null && (player.getMainHandItem().getItem() instanceof GunItem
                || player.getOffhandItem().getItem() instanceof GunItem)) {
            cir.setReturnValue(false);
        }
    }
}
