package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.input.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ttv.migami.jeg.item.GunItem;

/**
 * Each gun fires with the trigger of the hand that holds it. Vivecraft's default bindings put "attack" (JEG's shoot key)
 * on the main-hand trigger and "teleport" on the off-hand one, and it ships mirrored bindings for reversed hands. But
 * SteamVR keeps the binding the player already had, so after reversing hands in Vivecraft the attack trigger is often
 * still on the right controller, now the off hand: the triggers end up crossed. When SteamVR reports attack bound to
 * the off-hand controller, the two triggers swap roles for guns: the teleport key is the main-hand trigger (JEG's
 * shoot) and the attack key is the off-hand one ({@link VrOffhandShooting}).
 */
public final class VrTriggers {
    /**
     * Crossed: JEG's view of its shoot key. JEG fires semi-automatic guns once per press by releasing the shoot key
     * ({@code KeyMapping.setDown(false)}) after the shot; the teleport key it now reads is not the one it releases, so
     * a press is latched here and {@link #releaseMainTrigger} drops it, exactly like the real key.
     */
    private static boolean mainLatch;
    private static boolean mainWasDown;

    private VrTriggers() {
    }

    /** True when the attack key sits on the off-hand controller in the current SteamVR binding. */
    public static boolean crossed() {
        return VR.active() && VR.attackOnOffHand();
    }

    /** JEG's shoot key as JEG should see it: the trigger of the main-hand controller. */
    public static boolean mainTriggerDown(boolean shootKeyDown) {
        if (!crossed()) {
            mainLatch = false;
            mainWasDown = false;
            return shootKeyDown;
        }
        boolean down = teleportDown();
        if (down && !mainWasDown) {
            mainLatch = true;
        } else if (!down) {
            mainLatch = false;
        }
        mainWasDown = down;
        return mainLatch;
    }

    /** JEG released its shoot key (after a semi-automatic shot, or overheating). */
    public static void releaseMainTrigger() {
        mainLatch = false;
    }

    /** The trigger of the off-hand controller, read raw (hidden keys still count). */
    public static boolean offTriggerDown() {
        return crossed() ? raw(Minecraft.getInstance().options.keyAttack) : teleportDown();
    }

    /** Vivecraft's teleport keys must read as not pressed: they are a gun trigger right now. */
    public static boolean hidesTeleport() {
        if (!crossed()) {
            return VrOffhandShooting.triggerTakenOver();
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof GunItem;
    }

    /** The attack key must read as not pressed: crossed, it is the off-hand gun's trigger. */
    public static boolean hidesAttack(KeyMapping key) {
        return key == Minecraft.getInstance().options.keyAttack && crossed() && VrOffhandShooting.triggerTakenOver();
    }

    private static boolean teleportDown() {
        for (KeyMapping key : VR.teleportKeys()) {
            if (raw(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean raw(KeyMapping key) {
        return ((KeyMappingAccessor) key).jegvr$isDownRaw();
    }
}
