package com.jegvr.mixin.input;

import com.jegvr.client.VrTriggers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.handler.ShootingHandler;

/** JEG fires the main-hand gun while its shoot key is down; in VR that is the main-hand trigger ({@link VrTriggers}). */
@Mixin(value = ShootingHandler.class, remap = false)
public abstract class ShootingHandlerVRMixin {
    @WrapOperation(method = {"onHandleShooting", "onPostClientTick"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean jegvr$mainTrigger(KeyMapping key, Operation<Boolean> original) {
        boolean down = original.call(key);
        return key == KeyBinds.getShootMapping() ? VrTriggers.mainTriggerDown(down) : down;
    }
}
