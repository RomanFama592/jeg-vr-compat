package com.jegvr.client;

import com.jegvr.compat.VR;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Per-hand state for two guns in VR. JEG's first-person rendering assumes one gun: its muzzle flash shows on every gun
 * the player holds while shooting, and the animation controller reads the main-hand gun's tags for every gun. Vivecraft
 * renders each hand separately, so we remember which hand is being drawn and which hand fired.
 */
public final class VrHands {
    /** A flash is drawn for this many ticks after the shot (JEG shows it for about one tick). */
    private static final long FLASH_TICKS = 1;

    private static InteractionHand renderingHand;
    /** The hand whose gun the current JEG reload is for (JEG's RELOADING flag is per player). */
    private static InteractionHand reloadingHand = InteractionHand.MAIN_HAND;
    private static long mainShotTick = Long.MIN_VALUE;
    private static long offShotTick = Long.MIN_VALUE;

    private VrHands() {
    }

    /** Called by Vivecraft's per-hand item transform, right before a held gun is drawn. */
    public static void setRenderingHand(InteractionHand hand) {
        renderingHand = hand;
    }

    public static void markShot(InteractionHand hand) {
        long now = gameTime();
        if (hand == InteractionHand.OFF_HAND) {
            offShotTick = now;
        } else {
            mainShotTick = now;
        }
    }

    /**
     * Replacement for JEG's "is shooting" check when it decides to draw a muzzle flash: in VR first person, only the
     * gun of the hand that just fired flashes.
     */
    public static boolean shouldFlash(boolean original, ItemDisplayContext context) {
        if (!VR.active() || context == null || !context.firstPerson() || renderingHand == null) {
            return original;
        }
        long shot = renderingHand == InteractionHand.OFF_HAND ? offShotTick : mainShotTick;
        return gameTime() - shot <= FLASH_TICKS;
    }

    /**
     * The stack JEG's gun animation should read. It always reads the main-hand gun; while Vivecraft draws the off-hand
     * gun in first person, read that one instead.
     */
    public static ItemStack animatedStack(Player player, ItemStack mainHand) {
        return renderingOffhand(player) ? player.getOffhandItem() : mainHand;
    }

    /**
     * JEG's per-player "trigger held" flag, as the gun animation sees it. It belongs to the main-hand trigger, so the
     * off-hand gun (never charged from that hand) must not play its charge animation when the main gun fires.
     */
    public static boolean shootingFlag(Player player, boolean original) {
        return original && !renderingOffhand(player);
    }

    public static void markReload(InteractionHand hand) {
        reloadingHand = hand;
    }

    /**
     * JEG's per-player "reloading" flag, as the gun animation sees it. It does not say which gun reloads, so a reload of
     * one hand (even JEG's automatic one with no ammo for that gun) would animate the other hand's gun too: only the
     * gun of the hand that started the reload sees it.
     */
    public static boolean reloadingFlag(Player player, boolean original) {
        if (!original || player != Minecraft.getInstance().player || !VR.active()) {
            return original;
        }
        InteractionHand drawn = renderingHand == null ? InteractionHand.MAIN_HAND : renderingHand;
        return drawn == reloadingHand;
    }

    private static boolean renderingOffhand(Player player) {
        return renderingHand == InteractionHand.OFF_HAND && player == Minecraft.getInstance().player && VR.active();
    }

    private static long gameTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }
}
