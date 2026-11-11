package com.jegvr.mixin.vc12;

import com.jegvr.client.VrPrecision;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * Vivecraft 1.2 only. Precision mode ({@link VrPrecision#locksHotbar}): touching the hotbar with the hand selects
 * nothing. {@code processHotbar} runs every frame and finds the slot under the hand ({@code InteractTracker.hotbar}): a
 * touch selects it, and the off-hand slot swaps the items of both hands. It is skipped, leaving no slot under the hand,
 * as Vivecraft itself does while climbing: no highlight, no buzz, nothing to select.
 */
@Mixin(value = MCVR.class, remap = false)
public abstract class HotbarTouchVRMixin {
    @Inject(method = "processHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHotbarTouchWhileAiming(CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            ClientDataHolderVR.getInstance().interactTracker.hotbar = -1;
            ci.cancel();
        }
    }
}
