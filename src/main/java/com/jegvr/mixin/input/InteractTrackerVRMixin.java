package com.jegvr.mixin.input;

import com.jegvr.client.VrPrecision;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.gameplay.trackers.InteractTracker;

/**
 * Precision mode ({@link VrPrecision#locksHotbar}): Vivecraft's hand-on-hotbar selection is off. Touching a slot of the
 * hand's hotbar selects it, and touching the off-hand slot swaps the hands' items; either would end the two-handed grip.
 * Vivecraft finds the slot under the hand in {@code doProcess} and acts on it in {@code processBindings}: no slot is found.
 */
@Mixin(value = InteractTracker.class, remap = false)
public abstract class InteractTrackerVRMixin {
    @Shadow
    public int hotbar;

    @Inject(method = "doProcess", at = @At("TAIL"), require = 0)
    private void jegvr$noHotbarWhileAiming(LocalPlayer player, CallbackInfo ci) {
        if (VrPrecision.locksHotbar()) {
            this.hotbar = -1;
        }
    }
}
