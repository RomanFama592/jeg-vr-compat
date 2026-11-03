package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.server.VrOffhand;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.GunItem;

/**
 * Which hand holds the gun that hand loading ({@link VrGrabReload}) and precision mode ({@link VrPrecision}) act on;
 * the other hand is the free one that grabs magazines and the foregrip. Usually the gun is in the main hand. Holding it
 * in the off hand with the main hand empty works the same way, mirrored (F9.12); JEG cannot reload or fire the off-hand
 * gun, so that needs this addon on the server.
 */
public final class VrGunHand {
    private VrGunHand() {
    }

    /** The hand with the gun when the other hand is empty, or null. */
    public static InteractionHand of(LocalPlayer player) {
        if (player.getMainHandItem().getItem() instanceof GunItem && player.getOffhandItem().isEmpty()) {
            return InteractionHand.MAIN_HAND;
        }
        if (VrOffhand.canDualWield(player.getOffhandItem()) && player.getMainHandItem().isEmpty()
                && VrAim.serverHasAddon()) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static ItemStack stack(LocalPlayer player, InteractionHand hand) {
        return player.getItemInHand(hand);
    }

    public static VR.Aim aim(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? VR.mainHandAim() : VR.offHandAim();
    }

    public static void haptic(InteractionHand hand, float seconds, float amplitude) {
        VR.haptic(hand == InteractionHand.MAIN_HAND, seconds, amplitude);
    }

    /** The grip key of {@code hand} with Vivecraft's default bindings, or null without Vivecraft. */
    public static KeyMapping gripKey(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? VR.mainHandGripKey() : VR.offHandGripKey();
    }
}
