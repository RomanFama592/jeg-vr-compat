package com.jegvr.mixin.input;

import com.jegvr.client.VrGrabReload;
import com.jegvr.client.VrPrecision;
import com.jegvr.client.VrOffhandShooting;
import com.jegvr.client.VrThrow;
import com.jegvr.client.VrTriggers;
import net.minecraft.client.Minecraft;
import com.jegvr.compat.VR;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With a gun in the off hand, that hand's trigger fires it instead of teleporting: Vivecraft's teleport keys read as
 * not pressed. {@link VrOffhandShooting} reads them raw through {@link KeyMappingAccessor}. An off-hand grip press
 * that grabs a magazine ({@link VrGrabReload}) or takes the foregrip ({@link VrPrecision}) is consumed without
 * reaching Vivecraft (it would change hotbar slot). With crossed triggers ({@link VrTriggers}) the teleport key is the
 * main gun's trigger and the attack key the off-hand gun's, and they are hidden accordingly.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingVRMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void jegvr$hideTeleport(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping key = (KeyMapping) (Object) this;
        if ((VR.isTeleportKey(key) && VrTriggers.hidesTeleport()) || VrTriggers.hidesAttack(key)) {
            cir.setReturnValue(false);
        } else if (key == Minecraft.getInstance().options.keyUse && VrThrow.holdsUseKey()) {
            // F9.26: a grenade held by its trigger; Minecraft would stop using it (and JEG throw it) with the key up.
            cir.setReturnValue(true);
        }
    }

    /** Consumed and dropped, so hidden clicks do not pile up and fire once the key is no longer hidden. */
    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void jegvr$hideAttackClick(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && VrTriggers.hidesAttack((KeyMapping) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "consumeClick", at = @At("RETURN"), cancellable = true)
    private void jegvr$grabMagazine(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && VrGrabReload.isGripKey((KeyMapping) (Object) this) && (VrGrabReload.onGripPressed() || VrPrecision.onGripPressed())) {
            cir.setReturnValue(false);
        }
    }
}
