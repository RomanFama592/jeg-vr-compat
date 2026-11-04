package com.jegvr.mixin.render;

import com.jegvr.client.VrGunAnimations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import ttv.migami.jeg.item.AnimatedGunItem;

import java.util.Map;

/** F9.20: a JEG gun's controller moves no bones while {@link VrGunAnimations} says so; other GeckoLib models are untouched. */
@Mixin(value = AnimationProcessor.class, remap = false)
public abstract class AnimationProcessorVRMixin {
    // Ordinal 0 only clears the queues when resources reload.
    @WrapOperation(method = "tickAnimation", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lsoftware/bernie/geckolib/core/animation/AnimationController;getBoneAnimationQueues()Ljava/util/Map;"))
    private Map<String, BoneAnimationQueue> jegvr$dropHiddenMovement(AnimationController<?> controller,
                                                                     Operation<Map<String, BoneAnimationQueue>> original,
                                                                     @Local(argsOnly = true) GeoAnimatable animatable,
                                                                     @Local(argsOnly = true) AnimatableManager<?> manager) {
        Map<String, BoneAnimationQueue> queues = original.call(controller);
        return !(animatable instanceof AnimatedGunItem) || VrGunAnimations.movesBones(manager, controller) ? queues : Map.of();
    }
}
