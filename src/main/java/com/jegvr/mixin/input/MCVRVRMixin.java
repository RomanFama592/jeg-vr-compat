package com.jegvr.mixin.input;

import com.jegvr.client.VrPrecision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * Precision mode ({@link VrPrecision#locksHotbar}): Vivecraft's hotbar leaves the selected slot alone.
 * <ul>
 *   <li>{@code processHotbar} runs every frame and finds the slot under the hand ({@code InteractTracker.hotbar}): a
 *   touch selects it, and the off-hand slot swaps the hands' items. It is skipped, leaving no slot under the hand, as
 *   Vivecraft itself does while climbing: no highlight, no buzz, nothing to select.</li>
 *   <li>{@code changeHotbar}: the hotbar buttons, scroll and swipe.</li>
 * </ul>
 */
@Mixin(value = MCVR.class, remap = false)
public abstract class MCVRVRMixin {
    @Inject(method = "processHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHotbarTouchWhileAiming(CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            ClientDataHolderVR.getInstance().interactTracker.hotbar = -1;
            ci.cancel();
        }
    }

    @Inject(method = "changeHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHotbarChangeWhileAiming(int direction, CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            ci.cancel();
        }
    }
}
