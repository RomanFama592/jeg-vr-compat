package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.GripType;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageAim;

/**
 * Precision mode (S.7): put the free hand on the foregrip box of a two-handed gun and press the off-hand grip. While it
 * lasts the server sees the player as aiming, through JEG's own {@link C2SMessageAim} (so it also works on servers with
 * only JEG): JEG halves the spread and stops it from growing. The recoil deviation of {@link VrRecoil} is also smaller.
 * The client never aims down sights (JEG's isAiming stays false in VR), so nothing about the view changes.
 * <p>
 * Like a real rifle held with both hands, the gun points along the line from the main hand to the free hand: the
 * held gun is drawn that way ({@link VrGunPose}) and the shots follow it ({@link VrAim}).
 * <p>
 * It ends on a second grip press, when the free hand leaves the gun (too close, too far, or the line turns too far
 * from the gun hand's controller), on reload, or when the gun or the hands change.
 * <p>
 * The gun may also be in the off hand with the main hand free ({@link VrGunHand}): then the main hand takes the
 * foregrip and everything is mirrored.
 */
public final class VrPrecision {
    /** Room-scale metres: the box is drawn when the free hand is this close, and the mode ends when it goes farther. */
    private static final double SHOW_DISTANCE = 0.45;
    private static final int AIMING_RETRY_TICKS = 20;
    /** Recoil deviation and kick while in precision mode. */
    public static final float RECOIL_FACTOR = 0.4F;

    private static boolean active;
    private static int slot;
    /** The hand holding the gun while active. */
    private static InteractionHand hand = InteractionHand.MAIN_HAND;
    /** Ticks until asking the server again for its aiming flag, when it does not take it. */
    private static int aimingRetry;

    private VrPrecision() {
    }

    public static boolean active() {
        return active;
    }

    /** The off-hand grip was pressed (after grab-to-reload declined it). True when it toggled precision mode. */
    public static boolean onGripPressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (active) {
            stop(player);
            VrGunHand.haptic(VrGunHand.other(hand), 0.03F, 0.3F);
            return true;
        }
        InteractionHand gunHand = holdableHand(player);
        if (gunHand == null || !handOnForegrip(gunHand)) {
            return false;
        }
        active = true;
        hand = gunHand;
        VrGripHint.onGrab(gunHand);
        slot = player.getInventory().selected;
        sendAiming(player, true);
        // F10.4: a firm buzz ends the light one of being on the foregrip ({@link VrGripHint}).
        VrGunHand.haptic(VrGunHand.other(hand), 0.1F, 1.0F);
        VrGunHand.haptic(hand, 0.06F, 0.4F);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 1.4F);
        return true;
    }

    /**
     * While holding a gun with both hands, with {@code lockHotbarInPrecision} on: Vivecraft's hotbar (touch, buttons,
     * shoulder) and the swap-hands key do nothing, so nothing ends the grip by accident ({@code InteractTrackerVRMixin},
     * {@code MCVRVRMixin}, {@code BackpackTrackerVRMixin}).
     */
    public static boolean locksHotbar() {
        return active && VrClientConfig.lockHotbarInPrecision();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && locksHotbar()) {
            // Before Minecraft handles the keys this tick: the swap-hands key would move the gun to the other hand.
            KeyMapping swap = Minecraft.getInstance().options.keySwapOffhand;
            while (swap.consumeClick()) {
                VrGunHand.haptic(VrGunHand.other(hand), 0.02F, 0.2F);
            }
        }
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            active = false;
            return;
        }
        if (holdableHand(player) != hand || player.getInventory().selected != slot || twoHandDir(hand) == null) {
            stop(player);
        } else if (aimingRetry > 0) {
            aimingRetry--;
        } else if (!ModSyncedDataKeys.AIMING.getValue(player)) {
            // The server answered with its own value (e.g. after a respawn): ask again, without flooding it.
            aimingRetry = AIMING_RETRY_TICKS;
            sendAiming(player, true);
        }
    }

    private static void stop(LocalPlayer player) {
        active = false;
        VrGripHint.onRelease(hand);
        sendAiming(player, false);
    }

    private static void sendAiming(LocalPlayer player, boolean aiming) {
        ModSyncedDataKeys.AIMING.setValue(player, aiming);
        PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(aiming));
    }

    /**
     * VR with tracked hands, a two-handed gun in one hand ({@link VrGunHand}), the other hand free and no reload going
     * on: the hand with the gun, or null.
     */
    static InteractionHand holdableHand(LocalPlayer player) {
        if (!VR.active() || VR.seated() || player.isSpectator() || Minecraft.getInstance().screen != null
                || ModSyncedDataKeys.RELOADING.getValue(player)) {
            return null;
        }
        InteractionHand gunHand = VrGunHand.of(player);
        ItemStack stack = gunHand == null ? ItemStack.EMPTY : player.getItemInHand(gunHand);
        return stack.getItem() instanceof GunItem gunItem
                && !GripType.ONE_HANDED.equals(gunItem.getModifiedGun(stack).getGeneral().getGripType()) ? gunHand : null;
    }

    static Vec3 foregrip(InteractionHand gunHand) {
        VR.Aim gun = VrGunHand.aim(gunHand);
        if (gun == null) {
            return null;
        }
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return VrZones.foregrip(gun, free == null ? null : free.pos(), VR.worldScale());
    }

    private static AABB foregripBox(InteractionHand gunHand) {
        Vec3 center = foregrip(gunHand);
        return center == null ? null : VrZones.foregripBox(center, VR.worldScale());
    }

    /** While holding the foregrip, the box sits on the free hand (the gun turns towards it). */
    private static AABB heldBox() {
        VR.Aim free = VrGunHand.aim(VrGunHand.other(hand));
        return free == null ? null : VrZones.foregripBox(free.pos(), VR.worldScale());
    }

    static boolean handOnForegrip(InteractionHand gunHand) {
        AABB box = foregripBox(gunHand);
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return box != null && free != null && box.contains(free.pos());
    }

    /**
     * In precision mode with the gun in {@code shotHand}, that hand's aim with the direction from it to the free hand
     * (both hands on the gun); otherwise {@code aim} unchanged.
     */
    public static VR.Aim aim(InteractionHand shotHand, VR.Aim aim) {
        if (!active || shotHand != hand || aim == null) {
            return aim;
        }
        Vec3 dir = twoHandDir(hand);
        return dir == null ? aim : new VR.Aim(aim.pos(), dir);
    }

    /**
     * The two-hand direction in the axes of {@code drawnHand}'s controller, for drawing the gun held there; null unless
     * that gun is in precision mode.
     */
    public static Vec3 localAimDir(InteractionHand drawnHand) {
        Vec3 dir = active && drawnHand == hand ? twoHandDir(hand) : null;
        return dir == null ? null : VR.toHandLocal(hand == InteractionHand.MAIN_HAND, dir);
    }

    /** Unit vector from the gun hand to the free hand, or null when they are not holding one gun together. */
    private static Vec3 twoHandDir(InteractionHand gunHand) {
        VR.Aim gun = VrGunHand.aim(gunHand);
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return gun == null || free == null ? null : VrZones.twoHandDir(gun, free.pos(), VR.worldScale());
    }

    static double handDistance(InteractionHand gunHand) {
        Vec3 center = foregrip(gunHand);
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return center == null || free == null ? Double.MAX_VALUE : free.pos().distanceTo(center);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VrClientConfig.showGuides()
                || !VR.isEyePass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        InteractionHand gunHand = player == null ? null : active ? hand : holdableHand(player);
        if (gunHand == null || (!active && handDistance(gunHand) > SHOW_DISTANCE * VR.worldScale())) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        if (active) {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), heldBox(), 0.8F, 0.3F, 1.0F);
        } else if (handOnForegrip(gunHand)) {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), foregripBox(gunHand), 0.2F, 1.0F, 0.3F);
        } else {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), foregripBox(gunHand), 0.25F, 0.55F, 1.0F);
        }
        pose.popPose();
    }
}
