package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.jegvr.network.C2SVrAim;
import com.jegvr.network.C2SVrThrow;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrServerThrow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.MolotovCocktailItem;
import ttv.migami.jeg.item.WaterBombItem;

/**
 * F9.26: JEG's grenades, molotov and water bomb in VR, thrown with the trigger of the hand that holds them.
 * <ul>
 * <li>Press: pulls the pin. The vanilla "use item" with that hand, so JEG plays its pin sound, starts the fuse and the
 * server knows (works with a server that only has JEG).</li>
 * <li>Hold: the grenade cooks; the controller ticks like a fuse, faster near the end.</li>
 * <li>Release: throws. {@code TRIGGER}: where that hand points. {@code GESTURE}: along the hand's swing, as strong as the
 * swing when the server has this addon.</li>
 * </ul>
 * JEG only throws after 10 ticks of holding; a quicker release is kept and thrown when they are reached. The direction
 * reaches a JEG-only server as the player's rotation, sent right before the release; with this addon on the server the
 * grenade also leaves from the hand ({@code VrServerThrow}).
 */
public final class VrThrow {
    /** {@code GrenadeItem.releaseUsing} throws nothing before this many ticks of holding. */
    static final int MIN_HOLD_TICKS = 10;
    /** Fuse ticks: a light pulse every second, then faster in the last ones. */
    private static final int FUSE_SLOW_PERIOD = 20;
    private static final int FUSE_FAST_PERIOD = 5;
    private static final int FUSE_FAST_TICKS = 30;

    private static final VrThrowMotion MAIN_MOTION = new VrThrowMotion();
    private static final VrThrowMotion OFF_MOTION = new VrThrowMotion();

    /** The hand holding a grenade with the pin out; null otherwise. */
    private static InteractionHand hand;
    private static Item heldItem;
    /** Released before {@link #MIN_HOLD_TICKS}: the throw waits, with what the release decided. */
    private static boolean released;
    private static Vec3 releaseDir;
    /** Projectile speed for the server, or NaN for JEG's own. */
    private static float releaseSpeed = Float.NaN;
    private static boolean mainWasDown;
    private static boolean offWasDown;

    private VrThrow() {
    }

    public static boolean isThrowable(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof GrenadeItem || item instanceof MolotovCocktailItem || item instanceof WaterBombItem;
    }

    /** True when this addon handles throwing (VR, and the option is not Vivecraft's way). */
    public static boolean enabled() {
        return VrClientConfig.throwMode() != VrClientConfig.ThrowMode.VIVECRAFT && VR.active();
    }

    /** Vivecraft must not take {@code stack} for a bow ({@code BowTrackerVRMixin}). */
    public static boolean replacesBow(ItemStack stack) {
        return VrClientConfig.throwMode() != VrClientConfig.ThrowMode.VIVECRAFT && isThrowable(stack);
    }

    /** The trigger of {@code side} belongs to a throwable in that hand: the keys Vivecraft gave it read as not pressed. */
    public static boolean ownsTrigger(InteractionHand side) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && enabled() && (side == hand || isThrowable(player.getItemInHand(side)));
    }

    /**
     * While a grenade is held with the pin out, the use key must read as pressed: Minecraft stops using an item (and
     * JEG throws it) as soon as the use key is up.
     */
    public static boolean holdsUseKey() {
        LocalPlayer player = Minecraft.getInstance().player;
        return hand != null && player != null && player.isUsingItem() && player.getUsedItemHand() == hand;
    }

    /** Before Minecraft handles its keys this tick, so a release here comes before its own check of the use key. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || !enabled()) {
            reset();
            mainWasDown = false;
            offWasDown = false;
            MAIN_MOTION.clear();
            OFF_MOTION.clear();
            return;
        }
        MAIN_MOTION.push(VR.roomHandPosition(true));
        OFF_MOTION.push(VR.roomHandPosition(false));
        boolean mainDown = VrTriggers.mainTriggerRaw();
        boolean offDown = VrTriggers.offTriggerDown();
        if (hand == null) {
            if (mc.screen == null && !player.isUsingItem()) {
                if (mainDown && !mainWasDown && isThrowable(player.getMainHandItem())) {
                    pullPin(mc, player, InteractionHand.MAIN_HAND);
                } else if (offDown && !offWasDown && isThrowable(player.getOffhandItem())) {
                    pullPin(mc, player, InteractionHand.OFF_HAND);
                }
            }
        } else {
            tickHeld(mc, player, hand == InteractionHand.MAIN_HAND ? mainDown : offDown);
        }
        mainWasDown = mainDown;
        offWasDown = offDown;
    }

    private static void pullPin(Minecraft mc, LocalPlayer player, InteractionHand side) {
        VR.setActiveHand(side);
        mc.gameMode.useItem(player, side);
        if (player.isUsingItem() && player.getUsedItemHand() == side) {
            hand = side;
            heldItem = player.getItemInHand(side).getItem();
            released = false;
            VR.haptic(side == InteractionHand.MAIN_HAND, 0.05F, 0.6F);
        }
        VR.setActiveHand(InteractionHand.MAIN_HAND);
    }

    private static void tickHeld(Minecraft mc, LocalPlayer player, boolean triggerDown) {
        // It exploded in the hand, the slot changed, or something else stopped the use.
        if (!player.isUsingItem() || player.getUsedItemHand() != hand || player.getItemInHand(hand).getItem() != heldItem) {
            reset();
            return;
        }
        if (!released && !triggerDown) {
            decideRelease(player);
        }
        if (released && player.getTicksUsingItem() >= MIN_HOLD_TICKS) {
            throwNow(mc, player);
            return;
        }
        fuseTick(player);
    }

    /** What the release means: the direction, and the speed for the server (gesture only). */
    private static void decideRelease(LocalPlayer player) {
        boolean main = hand == InteractionHand.MAIN_HAND;
        VR.Aim aim = main ? VR.mainHandAim() : VR.offHandAim();
        Vec3 pointed = aim == null ? player.getLookAngle() : aim.dir();
        releaseDir = pointed;
        releaseSpeed = Float.NaN;
        if (VrClientConfig.throwMode() == VrClientConfig.ThrowMode.GESTURE) {
            Vec3 velocity = (main ? MAIN_MOTION : OFF_MOTION).peakVelocity();
            if (VrThrowMotion.isThrow(velocity)) {
                releaseDir = VrThrowMotion.toWorld(velocity, VR.roomYaw());
                releaseSpeed = VrThrowMotion.projectileSpeed(velocity.length(), VrServerThrow.MIN_SPEED,
                        VrServerThrow.MAX_SPEED);
            } else {
                // A slow release: a soft toss where the hand points.
                releaseSpeed = VrServerThrow.MIN_SPEED;
            }
        }
        released = true;
    }

    private static void throwNow(Minecraft mc, LocalPlayer player) {
        InteractionHand side = hand;
        boolean main = side == InteractionHand.MAIN_HAND;
        Vec3 dir = releaseDir.lengthSqr() < 1.0E-6 ? player.getLookAngle() : releaseDir.normalize();
        // A JEG-only server throws along the player's rotation: give it the throw's.
        float[] rotation = VrAim.rotation(dir, 0.0F, 0.0F);
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            connection.send(new ServerboundMovePlayerPacket.Rot(rotation[0], rotation[1], player.onGround()));
            if (VrAim.serverHasAddon()) {
                VR.Aim aim = main ? VR.mainHandAim() : VR.offHandAim();
                Vec3 from = aim == null ? player.getEyePosition() : aim.pos();
                VrNetwork.CHANNEL.sendToServer(new C2SVrAim(side, from, dir));
                if (!Float.isNaN(releaseSpeed)
                        && VrNetwork.serverProtocol(connection.getConnection()) >= VrNetwork.THROW_PROTOCOL) {
                    VrNetwork.CHANNEL.sendToServer(new C2SVrThrow(side, releaseSpeed));
                }
            }
        }
        reset();
        VR.setActiveHand(side);
        mc.gameMode.releaseUsingItem(player);
        VR.setActiveHand(InteractionHand.MAIN_HAND);
        VR.haptic(main, 0.08F, 0.8F);
    }

    /** The cooking grenade ticks in the hand like its fuse. */
    private static void fuseTick(LocalPlayer player) {
        if (!(heldItem instanceof GrenadeItem grenade) || !grenade.canCook()) {
            return;
        }
        int left = player.getUseItemRemainingTicks();
        int period = left <= FUSE_FAST_TICKS ? FUSE_FAST_PERIOD : FUSE_SLOW_PERIOD;
        if (left % period == 0) {
            VR.haptic(hand == InteractionHand.MAIN_HAND, 0.03F, left <= FUSE_FAST_TICKS ? 0.7F : 0.25F);
        }
    }

    private static void reset() {
        hand = null;
        heldItem = null;
        released = false;
        releaseDir = null;
        releaseSpeed = Float.NaN;
    }
}
