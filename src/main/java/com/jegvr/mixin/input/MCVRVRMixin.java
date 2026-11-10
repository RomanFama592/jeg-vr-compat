package com.jegvr.mixin.input;

import com.jegvr.client.VrPrecision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * Precision mode ({@link VrPrecision#locksHotbar}): Vivecraft's hotbar buttons, scroll and swipe ({@code changeHotbar})
 * leave the slot alone, so the two-handed grip is not ended by a stray press.
 */
@Mixin(value = MCVR.class, remap = false)
public abstract class MCVRVRMixin {
    @Inject(method = "changeHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHotbarChangeWhileAiming(int direction, CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            ci.cancel();
        }
    }
}
