package com.jegvr.mixin.input;

import com.jegvr.client.VrDualReload;
import com.jegvr.client.VrHands;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.ReloadHandler;

/** The reload and unload keys act on the gun whose barrel points up when there is one in each hand ({@link VrDualReload}). */
@Mixin(value = ReloadHandler.class, remap = false)
public abstract class ReloadHandlerVRMixin {
    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private void jegvr$reloadOffhand(InputEvent.Key event, CallbackInfo ci) {
        if (VrDualReload.onKeyPressed(event.getAction())) {
            ci.cancel();
        }
    }

    /** Every JEG reload start (key, automatic when empty, grab) is for the main-hand gun. */
    @Inject(method = "setReloading", at = @At("HEAD"))
    private void jegvr$mainHandReload(boolean reloading, CallbackInfo ci) {
        if (reloading) {
            VrHands.markReload(InteractionHand.MAIN_HAND);
        }
    }
}
