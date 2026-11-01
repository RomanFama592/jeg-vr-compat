package com.jegvr.mixin.input;

import com.jegvr.compat.VR;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.handler.AimingHandler;

/**
 * Aiming down sights means nothing in VR (the gun follows the controller, the zoom would be applied to the headset
 * view), so in VR JEG never aims; its aim key reloads instead ({@code VrDualReload}).
 */
@Mixin(value = AimingHandler.class, remap = false)
public abstract class AimingHandlerVRMixin {
    @Inject(method = "isAiming", at = @At("HEAD"), cancellable = true)
    private void jegvr$noAimInVr(CallbackInfoReturnable<Boolean> cir) {
        if (VR.active()) {
            cir.setReturnValue(false);
        }
    }
}
