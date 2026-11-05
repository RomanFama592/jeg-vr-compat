package com.jegvr.mixin.input;

import com.jegvr.client.VrDualReload;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.InspectHandler;

/** The inspect key inspects the gun whose barrel points up when there is one in each hand ({@link VrDualReload}). */
@Mixin(value = InspectHandler.class, remap = false)
public abstract class InspectHandlerVRMixin {
    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private void jegvr$inspectOffhand(InputEvent.Key event, CallbackInfo ci) {
        if (VrDualReload.onInspectKey(event.getAction())) {
            ci.cancel();
        }
    }
}
