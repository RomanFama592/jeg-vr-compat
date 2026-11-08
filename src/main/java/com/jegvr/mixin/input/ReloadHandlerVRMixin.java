package com.jegvr.mixin.input;

import com.jegvr.client.VrDualReload;
import com.jegvr.client.VrHands;
import com.jegvr.compat.jeg.JegKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.init.ModSyncedDataKeys;

/** The reload and unload keys act on the gun whose barrel points up when there is one in each hand ({@link VrDualReload}). */
@Mixin(value = ReloadHandler.class, remap = false)
public abstract class ReloadHandlerVRMixin {
    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private void jegvr$reloadOffhand(InputEvent.Key event, CallbackInfo ci) {
        if (VrDualReload.onKeyPressed(event.getAction())) {
            ci.cancel();
        }
    }

    /**
     * Every JEG reload start (key, automatic when empty, grab) is for the main-hand gun. F9.25: refused while each hand
     * holds a gun and the option says so ({@link VrDualReload#reloadBlocked}).
     */
    @Inject(method = "setReloading", at = @At("HEAD"), cancellable = true)
    private void jegvr$mainHandReload(boolean reloading, CallbackInfo ci) {
        if (!reloading) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && VrDualReload.reloadBlocked(player)) {
            // JEG's reload key marks the gun as reloading just before this call; undo it unless a reload is running.
            CompoundTag tag = player.getMainHandItem().getTag();
            if (tag != null && !ModSyncedDataKeys.RELOADING.getValue(player)) {
                tag.putBoolean(JegKeys.IS_RELOADING, false);
            }
            VrDualReload.refuseReload(true);
            ci.cancel();
            return;
        }
        VrHands.markReload(InteractionHand.MAIN_HAND);
    }
}
