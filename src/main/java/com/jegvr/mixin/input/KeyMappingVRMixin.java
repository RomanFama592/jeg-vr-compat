package com.jegvr.mixin.input;

import com.jegvr.client.VrGrabReload;
import com.jegvr.client.VrOffhandShooting;
import com.jegvr.compat.VR;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With a gun in the off hand, that hand's trigger fires it instead of teleporting: Vivecraft's teleport keys read as
 * not pressed. {@link VrOffhandShooting} reads them raw through {@link KeyMappingAccessor}. An off-hand grip press
 * that grabs a magazine ({@link VrGrabReload}) is consumed without reaching Vivecraft (it would change hotbar slot).
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingVRMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void jegvr$hideTeleport(CallbackInfoReturnable<Boolean> cir) {
        if (VrOffhandShooting.triggerTakenOver() && VR.isTeleportKey((KeyMapping) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void jegvr$grabMagazine(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && VrGrabReload.isGripKey((KeyMapping) (Object) this) && VrGrabReload.onGripPressed()) {
            cir.setReturnValue(false);
        }
    }
}
