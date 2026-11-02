package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * It ends on a second grip press, when the free hand leaves the gun, on reload, or when the gun or the hands change.
 */
public final class VrPrecision {
    /** Room-scale metres (multiplied by Vivecraft's world scale), along the main-hand aim from the controller. */
    private static final double FOREGRIP_FORWARD = 0.3;
    private static final double FOREGRIP_DOWN = 0.02;
    private static final Vec3 FOREGRIP_HALF = new Vec3(0.08, 0.08, 0.08);
    /** The box is drawn when the free hand is this close, and the mode ends when the hand goes farther. */
    private static final double SHOW_DISTANCE = 0.45;
    private static final double RELEASE_DISTANCE = 0.35;
    /** Recoil deviation and kick while in precision mode. */
    public static final float RECOIL_FACTOR = 0.4F;

    private static boolean active;
    private static int slot;

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
            VR.haptic(false, 0.03F, 0.3F);
            return true;
        }
        if (!canHold(player) || !handOnForegrip()) {
            return false;
        }
        active = true;
        slot = player.getInventory().selected;
        sendAiming(player, true);
        VR.haptic(false, 0.05F, 0.5F);
        VR.haptic(true, 0.05F, 0.3F);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 0.5F, 1.4F);
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            active = false;
            return;
        }
        if (!canHold(player) || player.getInventory().selected != slot || handDistance() > RELEASE_DISTANCE * VR.worldScale()) {
            stop(player);
        } else if (!ModSyncedDataKeys.AIMING.getValue(player)) {
            // The server answered with its own value (e.g. after a respawn): ask again.
            sendAiming(player, true);
        }
    }

    private static void stop(LocalPlayer player) {
        active = false;
        sendAiming(player, false);
    }

    private static void sendAiming(LocalPlayer player, boolean aiming) {
        ModSyncedDataKeys.AIMING.setValue(player, aiming);
        PacketHandler.getPlayChannel().sendToServer(new C2SMessageAim(aiming));
    }

    /** VR with tracked hands, a two-handed gun in the main hand, the off hand free and no reload going on. */
    private static boolean canHold(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return VR.active() && !VR.seated() && !player.isSpectator() && Minecraft.getInstance().screen == null
                && stack.getItem() instanceof GunItem gunItem && player.getOffhandItem().isEmpty()
                && !GripType.ONE_HANDED.equals(gunItem.getModifiedGun(stack).getGeneral().getGripType())
                && !ModSyncedDataKeys.RELOADING.getValue(player);
    }

    private static Vec3 foregrip() {
        VR.Aim main = VR.mainHandAim();
        if (main == null || main.dir().lengthSqr() < 1.0E-6) {
            return null;
        }
        double scale = VR.worldScale();
        return main.pos().add(main.dir().normalize().scale(FOREGRIP_FORWARD * scale)).add(0.0, -FOREGRIP_DOWN * scale, 0.0);
    }

    private static AABB foregripBox() {
        Vec3 center = foregrip();
        if (center == null) {
            return null;
        }
        Vec3 h = FOREGRIP_HALF.scale(VR.worldScale());
        return new AABB(center.subtract(h), center.add(h));
    }

    private static boolean handOnForegrip() {
        AABB box = foregripBox();
        VR.Aim hand = VR.offHandAim();
        return box != null && hand != null && box.contains(hand.pos());
    }

    private static double handDistance() {
        Vec3 center = foregrip();
        VR.Aim hand = VR.offHandAim();
        return center == null || hand == null ? Double.MAX_VALUE : hand.pos().distanceTo(center);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VrClientConfig.showGuides()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !canHold(player) || (!active && handDistance() > SHOW_DISTANCE * VR.worldScale())) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        if (active) {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), foregripBox(), 0.8F, 0.3F, 1.0F);
        } else if (handOnForegrip()) {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), foregripBox(), 0.2F, 1.0F, 0.3F);
        } else {
            VrGrabReload.drawBox(pose, mc.renderBuffers().bufferSource(), foregripBox(), 0.25F, 0.55F, 1.0F);
        }
        pose.popPose();
    }
}
