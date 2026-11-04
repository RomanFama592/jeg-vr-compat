package com.jegvr.client;

import com.jegvr.compat.VR;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;

import java.util.Set;

/**
 * F9.20: in VR the guns only move for JEG's reload and inspect animations. The others (shooting, sprinting, drawing,
 * melee...) keep playing unseen, because their keyframes also eject casings and play sounds and JEG reads them, but
 * their bone movement is dropped ({@code AnimationProcessorVRMixin}). The gun then rests in its idle pose, played by a
 * second controller ({@code AnimatedGunItemVRMixin}): the idle animation also hides parts of some guns (the spare rocket
 * of the launcher, for instance), so simply not moving the bones would show them.
 */
public final class VrGunAnimations {
    public static final String IDLE_CONTROLLER = "jegvr_idle";
    /** The name JEG gives its controller ({@code GunAnimations.animationController}). */
    private static final String JEG_CONTROLLER = "Controller";
    private static final Set<String> KEPT = Set.of("idle", "reload", "reload_alt", "reload_start", "reload_loop",
            "reload_stop", "inspect");

    private VrGunAnimations() {
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
