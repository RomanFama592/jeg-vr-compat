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
    private static final int INSPECT_TICKS = 5;
    private static final String INSPECTING_TAG = "IsInspecting";
    private static final String DRAWING_TAG = "IsDrawing";

    private static InteractionHand renderingHand;
    /** The hand whose gun the current JEG reload is for (JEG's RELOADING flag is per player). */
    private static InteractionHand reloadingHand = InteractionHand.MAIN_HAND;
    /** The hand of the current or last reload, which {@link #reloadingHand} forgets as soon as it ends. */
    private static InteractionHand lastReloadHand = InteractionHand.MAIN_HAND;
    /** JEG inspects the gun whose tag says so for a moment, but it clears the tag of the off-hand gun every tick. */
    private static int offhandInspectTicks;
    /** The same for the draw animation, whose tag JEG only keeps on the main-hand gun. */
    private static int offhandDrawTicks;
    private static long mainShotTick = Long.MIN_VALUE;
    private static long offShotTick = Long.MIN_VALUE;

    private VrHands() {
    }

    /** Called by Vivecraft's per-hand item transform, right before a held gun is drawn. */
    public static void setRenderingHand(InteractionHand hand) {
        renderingHand = hand;
    }

    /** The hand whose gun is being drawn, or the last one drawn; null before the first. */
    public static InteractionHand renderingHand() {
        return renderingHand;
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
     * off-hand gun reads its own trigger: it plays its charge animation only while that one charges it.
     */
    public static boolean shootingFlag(Player player, boolean original) {
        return renderingOffhand(player) ? VrOffhandShooting.triggerHeld() : original;
    }

    public static void markReload(InteractionHand hand) {
        reloadingHand = hand;
    }

    /** Called every client tick. */
    public static void tick(boolean reloading) {
        if (reloading) {
            lastReloadHand = reloadingHand;
        } else {
            reloadingHand = InteractionHand.MAIN_HAND;
        }
        if (offhandInspectTicks > 0) {
            offhandInspectTicks--;
        }
        if (offhandDrawTicks > 0) {
            offhandDrawTicks--;
        }
    }

    public static InteractionHand lastReloadHand() {
        return lastReloadHand;
    }

    /** Starts the inspect animation of the off-hand gun; as long as JEG's own tag lasts on a main-hand gun. */
    public static void inspectOffhand() {
        offhandInspectTicks = INSPECT_TICKS;
    }

    /** Starts the draw animation of the off-hand gun, as long as JEG's own tag would last on a main-hand gun. */
    public static void drawOffhand(int ticks) {
        offhandInspectTicks = 0;
        offhandDrawTicks = ticks;
    }

    /** A boolean of a gun's tag, as the gun animation sees it. */
    public static boolean tagFlag(Player player, String key, boolean original) {
        if (original || !renderingOffhand(player)) {
            return original;
        }
        return (offhandInspectTicks > 0 && INSPECTING_TAG.equals(key)) || (offhandDrawTicks > 0 && DRAWING_TAG.equals(key));
    }

    public static InteractionHand reloadingHand() {
        return reloadingHand;
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
