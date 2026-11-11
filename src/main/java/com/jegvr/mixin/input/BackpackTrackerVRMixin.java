package com.jegvr.mixin.input;

import com.jegvr.client.VrPrecision;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.gameplay.trackers.BackpackTracker;

/**
 * Precision mode ({@link VrPrecision#locksHotbar}): reaching over the shoulder does not change the hotbar slot. With a
 * rifle shouldered the gun hand is right there, and the new slot would end the two-handed grip.
 */
@Mixin(value = BackpackTracker.class, remap = false)
public abstract class BackpackTrackerVRMixin {
    // Vivecraft 1.2 names it doProcess; 1.3, activeProcess.
    @Inject(method = {"doProcess", "activeProcess"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noBackpackWhileAiming(LocalPlayer player, CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            ci.cancel();
        }
    }
}
