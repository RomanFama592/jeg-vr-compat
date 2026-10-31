package com.jegvr.mixin.recoil;

import com.jegvr.compat.VR;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.GunRecoilHandler;

/** JEG recoil rotates the player's camera; in VR that causes motion sickness, so VrRecoil takes over. */
@Mixin(value = GunRecoilHandler.class, remap = false)
public class GunRecoilVRMixin {
    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true)
    private void jegvr$noCameraRecoil(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        if (VR.active()) {
            ci.cancel();
        }
    }
}
