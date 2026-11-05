package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import ttv.migami.jeg.item.AnimatedGunItem;

import java.util.Set;

/**
 * F9.20: in VR the guns do not move for JEG's sprint, melee, bayonet and jam animations. Those keep playing unseen,
 * because their keyframes can also eject casings and play sounds and JEG reads them, but their bone movement is dropped
 * ({@code AnimationProcessorVRMixin}). The gun then rests in its idle pose, played by a
 * second controller ({@code AnimatedGunItemVRMixin}): the idle animation also hides parts of some guns (the spare rocket
 * of the launcher, for instance), so simply not moving the bones would show them.
 */
public final class VrGunAnimations {
    public static final String IDLE_CONTROLLER = "jegvr_idle";
    /** The name JEG gives its controller ({@code GunAnimations.animationController}). */
    private static final String JEG_CONTROLLER = "Controller";
    private static final Set<String> KEPT = Set.of("idle", "reload", "reload_alt", "reload_start", "reload_loop",
            "reload_stop", "inspect", "shoot", "aim_shoot", "draw", "hold_fire", "hold");

    /** Phase H: the animations the hands follow. The idle pose is not one: there the hands stay with the controllers. */
    private static final Set<String> FOLLOWED = Set.of("reload", "reload_alt", "reload_start", "reload_loop",
            "reload_stop", "inspect", "shoot", "aim_shoot", "draw", "hold_fire", "hold");
    /** Phase H: of those, the ones without shots, where the controller carries the gun with a delay. */
    private static final Set<String> HEAVY = Set.of("reload", "reload_alt", "reload_start", "reload_loop",
            "reload_stop", "inspect", "draw");
    /** Phase H: the animations of shots, which leave the other hand with its controller. */
    private static final Set<String> FIRING = Set.of("shoot", "aim_shoot", "hold_fire", "hold");

    private VrGunAnimations() {
    }

    /** The name of the animation this gun plays that the hands follow, or null. */
    public static String followedAnimation(ItemStack stack) {
        if (!(stack.getItem() instanceof AnimatedGunItem gun)) {
            return null;
        }
        AnimationController<?> jeg = gun.getAnimatableInstanceCache().getManagerForId(GeoItem.getId(stack))
                .getAnimationControllers().get(JEG_CONTROLLER);
        AnimationProcessor.QueuedAnimation current = jeg == null ? null : jeg.getCurrentAnimation();
        if (current == null || jeg.getAnimationState() == AnimationController.State.STOPPED) {
            return null;
        }
        return FOLLOWED.contains(current.animation().name()) ? current.animation().name() : null;
    }

    public static boolean isHeavy(String animation) {
        return animation != null && HEAVY.contains(animation);
    }

    public static boolean isFiring(String animation) {
        return animation != null && FIRING.contains(animation);
    }

    /** Whether this controller of a JEG gun moves the bones now: JEG's, or instead of it the idle one. */
    public static boolean movesBones(AnimatableManager<?> manager, AnimationController<?> controller) {
        boolean hidden = jegAnimationHidden(manager);
        return IDLE_CONTROLLER.equals(controller.getName()) ? hidden : !hidden;
    }

    private static boolean jegAnimationHidden(AnimatableManager<?> manager) {
        if (!VR.active()) {
            return false;
        }
        AnimationController<?> jeg = manager.getAnimationControllers().get(JEG_CONTROLLER);
        AnimationProcessor.QueuedAnimation current = jeg == null ? null : jeg.getCurrentAnimation();
        return current != null && !KEPT.contains(current.animation().name());
    }
}
