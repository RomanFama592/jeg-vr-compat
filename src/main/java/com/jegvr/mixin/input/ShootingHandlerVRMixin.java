package com.jegvr.mixin.input;

import com.jegvr.client.VrBows;
import com.jegvr.client.VrTriggers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.handler.ShootingHandler;

/**
 * JEG fires the main-hand gun while its shoot key is down; in VR that is the main-hand trigger ({@link VrTriggers}).
 * A JEG bow in Vivecraft's roomscale bow ({@link VrBows}) ignores the trigger: the key is "down" while the string is
 * drawn, so JEG charges it and fires on release.
 */
@Mixin(value = ShootingHandler.class, remap = false)
public abstract class ShootingHandlerVRMixin {
    @WrapOperation(method = {"onHandleShooting", "onPostClientTick"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean jegvr$mainTrigger(KeyMapping key, Operation<Boolean> original) {
        boolean down = original.call(key);
        if (key != KeyBinds.getShootMapping()) {
            return down;
        }
        return VrBows.active() ? VrBows.drawing() : VrTriggers.mainTriggerDown(down);
    }

    /** JEG fires semi-automatic guns once per press by releasing the shoot key; release ours too. */
    @WrapOperation(method = "onPostClientTick", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private void jegvr$releaseTrigger(KeyMapping key, boolean down, Operation<Void> original) {
        original.call(key, down);
        if (!down && key == KeyBinds.getShootMapping()) {
            VrTriggers.releaseMainTrigger();
        }
    }
}
