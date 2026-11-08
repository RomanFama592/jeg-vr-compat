package com.jegvr.mixin.input;

import com.jegvr.client.VrThrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.gameplay.trackers.BowTracker;

/**
 * F9.26, G.1: Vivecraft takes every item with the bow use animation for a bow, JEG's grenades, molotov and water bomb
 * included: it draws them in the off hand and throws them by drawing a string. With {@link VrThrow} on they are held and
 * thrown like what they are. A client mixin rather than the {@code vivecraft:bow_exclusion} tag: item tags come from the
 * server, and a server with only JEG does not have ours.
 */
@Mixin(value = BowTracker.class, remap = false)
public abstract class BowTrackerVRMixin {
    @Inject(method = "isBow", at = @At("HEAD"), cancellable = true)
    private static void jegvr$grenadesAreNotBows(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (VrThrow.replacesBow(stack)) {
            cir.setReturnValue(false);
        }
    }
}
