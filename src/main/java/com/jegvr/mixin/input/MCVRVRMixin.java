package com.jegvr.mixin.input;

import com.jegvr.client.VrPrecision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * Precision mode ({@link VrPrecision#locksHotbar}): Vivecraft's hotbar leaves the selected slot alone. Here,
 * {@code changeHotbar}: the hotbar buttons, scroll and swipe. Touching a slot with the hand is in a different place in
 * each line of Vivecraft: see {@code mixin.vc12.HotbarTouchVRMixin} and {@code mixin.vc13.HotbarTouchVRMixin}.
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
