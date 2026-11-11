package com.jegvr.mixin.vc13;

import com.jegvr.client.VrPrecision;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.gameplay.interact_modules.InteractiveHotbarModule;

/**
 * Vivecraft 1.3 only. Precision mode ({@link VrPrecision#locksHotbar}): touching the hotbar with the hand selects
 * nothing. In 1.3 the touch is an interact module: {@code isActive} finds the slot under the hand, after {@code reset}
 * left none. Answering no before it looks leaves no slot under the hand: no highlight, no buzz, nothing to select.
 */
@Mixin(value = InteractiveHotbarModule.class, remap = false)
public abstract class HotbarTouchVRMixin {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$noHotbarTouchWhileAiming(LocalPlayer player, InteractionHand hand, Vec3 handPosition,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (VrPrecision.locksHotbar()) {
            cir.setReturnValue(false);
        }
    }
}
