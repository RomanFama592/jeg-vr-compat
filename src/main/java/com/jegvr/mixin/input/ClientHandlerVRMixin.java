package com.jegvr.mixin.input;

import com.jegvr.client.VrDualReload;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.ClientHandler;

/** The attachments key opens the screen of the gun whose barrel points up when there is one in each hand ({@link VrDualReload}). */
@Mixin(value = ClientHandler.class, remap = false)
public abstract class ClientHandlerVRMixin {
    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private static void jegvr$attachmentsOffhand(InputEvent.Key event, CallbackInfo ci) {
        if (VrDualReload.onAttachmentsKey(event.getAction())) {
            ci.cancel();
        }
    }
}
