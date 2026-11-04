package com.jegvr.mixin.render;

import com.jegvr.client.VrGunAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import ttv.migami.jeg.animations.GunAnimations;
import ttv.migami.jeg.item.AnimatedGunItem;

/** F9.20: a controller that always plays the idle pose; it only moves the bones when {@link VrGunAnimations} says so. */
@Mixin(value = AnimatedGunItem.class, remap = false)
public abstract class AnimatedGunItemVRMixin {
    @Inject(method = "registerControllers", at = @At("TAIL"))
    private void jegvr$idleController(AnimatableManager.ControllerRegistrar controllers, CallbackInfo ci) {
        controllers.add(new AnimationController<>((AnimatedGunItem) (Object) this, VrGunAnimations.IDLE_CONTROLLER, 0,
                state -> state.setAndContinue(GunAnimations.IDLE)));
    }
}
