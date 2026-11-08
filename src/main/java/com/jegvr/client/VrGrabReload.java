package com.jegvr.client;

import com.jegvr.compat.jeg.JegKeys;
import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.jegvr.network.C2SVrInsertRound;
import com.jegvr.network.VrNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Reload by hand (phase 7): with the off hand free, put it in the hip box and press the off-hand grip to take a
 * magazine; the magazine box then follows the hand until it touches the gun's magazine-well box. That starts JEG's
 * normal reload (works on servers with only JEG). Guns loaded round by round ({@code MANUAL}, shotguns) take one round
 * per grab: with this addon on the server the round goes in at once ({@code C2SVrInsertRound}), otherwise JEG's reload
 * is started and stopped after one round. JEG's reload key keeps working.
 * <p>
 * Guides (off by default; {@code showGuides} in the client config, {@code /jegvr zones} toggles them): the hip box is red when there is no ammo, blue when a
 * magazine can be taken and green with the hand in it; while holding, the magazine box is yellow and the
 * magazine-well box turns green on contact.
 * <p>
 * The gun may also be in the off hand with the main hand empty ({@link VrGunHand}, needs the addon on the server): the
 * main hand then takes the magazine from the hip on its own side and loads the off-hand gun.
 * <p>
 * The off-hand grip is Vivecraft's "hotbar" key: a press that grabs (or drops) a magazine is taken from Vivecraft
 * ({@code KeyMappingVRMixin}), anywhere else it still changes the hotbar slot. {@link VrKeys#GRAB} is an optional
 * alternative for custom bindings.
 */
public final class VrGrabReload {
    private static final int TIMEOUT_TICKS = 200;
    /** Muzzle-up kick of the gun model when a round goes in by hand. */
    private static final float ROUND_JOLT_DEGREES = 6.0F;

    private enum Pouch { HIDDEN, NO_AMMO, READY }

    private static boolean holding;
    /** While holding: the hand with the gun being loaded (the other hand holds the magazine). */
    private static InteractionHand heldGunHand = InteractionHand.MAIN_HAND;
    private static int heldTicks;
    private static int slot;
    private static boolean grabWasDown;
    private static boolean handWasInPouch;
    /** MANUAL reload started by a grab: stop it once the gun has more rounds than this. */
    private static int singleRoundFrom = -1;

    private VrGrabReload() {
    }

    /** {@code /jegvr zones}: show or hide the hip and magazine-well boxes (saved in the client config). */
    public static boolean toggleZones() {
        boolean show = !VrClientConfig.showGuides();
        VrClientConfig.setShowGuides(show);
        return show;
    }

    /**
     * The off-hand grip was pressed (from {@code KeyMappingVRMixin}). Returns true when the press grabbed or dropped a
     * magazine, so Vivecraft must not also use it.
     */
    public static boolean onGripPressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (holding) {
            // A second press puts the magazine back.
            holding = false;
            VrGunHand.haptic(VrGunHand.other(heldGunHand), 0.03F, 0.3F);
            return true;
        }
        if (player == null || pouchState(player) != Pouch.READY || !handInPouch(player)) {
            return false;
        }
        holding = true;
        heldGunHand = VrGunHand.of(player);
        heldTicks = 0;
        slot = player.getInventory().selected;
        VrGunHand.haptic(VrGunHand.other(heldGunHand), 0.05F, 0.6F);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 0.6F, 1.3F);
        return true;
    }

    /** The grip key of the free hand (the off hand, or the main hand when the gun is in the off hand). */
    public static boolean isGripKey(KeyMapping key) {
        LocalPlayer player = Minecraft.getInstance().player;
        InteractionHand gunHand = holding ? heldGunHand : player == null ? null : VrGunHand.of(player);
        return gunHand != null && key == VrGunHand.gripKey(VrGunHand.other(gunHand));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        boolean grabDown = VrKeys.GRAB.isDown();
        if (grabDown && !grabWasDown && !onGripPressed()) {
            VrPrecision.onGripPressed();
        }
        grabWasDown = grabDown;
        if (player == null || !VR.active()) {
            holding = false;
            handWasInPouch = false;
            singleRoundFrom = -1;
            return;
        }
        tickSingleRound(player);
        if (holding) {
            tickHolding(player);
        } else {
            // A light buzz when the hand enters the hip box, so it can be found without looking.
            boolean inPouch = pouchState(player) == Pouch.READY && handInPouch(player);
            if (inPouch && !handWasInPouch) {
                VrGunHand.haptic(VrGunHand.other(VrGunHand.of(player)), 0.02F, 0.25F);
            }
            handWasInPouch = inPouch;
        }
    }

    private static void tickHolding(LocalPlayer player) {
        if (++heldTicks > TIMEOUT_TICKS || player.getInventory().selected != slot || VrGunHand.of(player) != heldGunHand
                || !canGrab(player)) {
            holding = false;
            return;
        }
        if (!magazineTouchesGun(heldGunHand)) {
            return;
        }
        holding = false;
        ItemStack stack = player.getItemInHand(heldGunHand);
        GunItem gunItem = (GunItem) stack.getItem();
        VR.haptic(true, 0.08F, 0.8F);
        VR.haptic(false, 0.08F, 0.8F);
        boolean main = heldGunHand == InteractionHand.MAIN_HAND;
        // The rocket launcher holds one rocket: loading it by hand plays JEG's whole reload, like its reload key.
        if (ReloadType.MANUAL.equals(gunItem.getModifiedGun(stack).getReloads().getReloadType())
                && !stack.is(ModItems.ROCKET_LAUNCHER.get())) {
            if (VrAim.serverHasAddon()) {
                // F7.8: the server loads this round at once (no JEG reload, no wait); the gun gives a short jolt.
                VrNetwork.CHANNEL.sendToServer(new C2SVrInsertRound(heldGunHand));
                if (main) {
                    VrRecoil.jolt(ROUND_JOLT_DEGREES);
                }
                return;
            }
            // JEG-only server: start JEG's reload and stop it after one round (slower).
            singleRoundFrom = ammo(stack);
        }
        if (main) {
            VrDualReload.reloadMainHand(player);
        } else {
            VrDualReload.reloadOffhand(player);
        }
    }

    /** JEG keeps loading rounds while reloading; a grab inserts one, so stop the reload after it. */
    private static void tickSingleRound(LocalPlayer player) {
        if (singleRoundFrom < 0) {
            return;
        }
        boolean reloading = ModSyncedDataKeys.RELOADING.getValue(player);
        if (!reloading || !(player.getMainHandItem().getItem() instanceof GunItem)) {
            singleRoundFrom = -1;
        } else if (ammo(player.getMainHandItem()) > singleRoundFrom) {
            singleRoundFrom = -1;
            ReloadHandler.get().setReloading(false);
        }
    }

    private static boolean canGrab(LocalPlayer player) {
        return pouchState(player) == Pouch.READY;
    }

    /**
     * READY: one hand free and a gun in the other ({@link VrGunHand}) that is reloaded (not inventory fed), not full,
     * and ammo to load. NO_AMMO: the same without ammo. HIDDEN: anything else.
     */
    private static Pouch pouchState(LocalPlayer player) {
        InteractionHand gunHand = VrGunHand.of(player);
        ItemStack stack = gunHand == null ? ItemStack.EMPTY : player.getItemInHand(gunHand);
        if (!VR.active() || VR.seated() || VrClientConfig.grabZone() == VrClientConfig.GrabZone.OFF
                || player.isSpectator() || Minecraft.getInstance().screen != null
                || !(stack.getItem() instanceof GunItem gunItem)
                || ModSyncedDataKeys.RELOADING.getValue(player)) {
            return Pouch.HIDDEN;
        }
        Gun gun = gunItem.getModifiedGun(stack);
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getBoolean(JegKeys.IGNORE_AMMO)
                || ReloadType.INVENTORY_FED.equals(gun.getReloads().getReloadType())
                || ammo(stack) >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return Pouch.HIDDEN;
        }
        boolean hasAmmo = player.isCreative() || !Gun.findAmmo(player, ammoItem(gun)).stack().isEmpty();
        return hasAmmo ? Pouch.READY : Pouch.NO_AMMO;
    }

    /** The item JEG looks for in the inventory: the reload item for SINGLE_ITEM guns, the round for the rest. */
    private static ResourceLocation ammoItem(Gun gun) {
        return ReloadType.SINGLE_ITEM.equals(gun.getReloads().getReloadType())
                ? gun.getReloads().getReloadItem() : gun.getProjectile().getItem();
    }

    private static int ammo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(JegKeys.AMMO_COUNT);
    }

    private static boolean handInPouch(LocalPlayer player) {
        InteractionHand gunHand = VrGunHand.of(player);
        if (gunHand == null) {
            return false;
        }
        AABB pouch = pouchBox(gunHand);
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return pouch != null && free != null && pouch.contains(free.pos());
    }

    private static boolean magazineTouchesGun(InteractionHand gunHand) {
        AABB magazine = magazineBox(gunHand);
        AABB magwell = magwellBox(gunHand);
        return magazine != null && magwell != null && magazine.intersects(magwell);
    }

    /** Hip (or, seated, belly) on the free hand's side, following the headset's yaw. */
    private static AABB pouchBox(InteractionHand gunHand) {
        VR.Aim hmd = VR.hmdAim();
        if (hmd == null) {
            return null;
        }
        // The off hand is the left one unless hands are reversed; the free hand is the one without the gun.
        boolean freeHandOnLeft = (gunHand == InteractionHand.MAIN_HAND) != VR.leftHanded();
        return VrZones.pouch(hmd, VrClientConfig.grabZone() == VrClientConfig.GrabZone.FRONT, freeHandOnLeft,
                VR.worldScale());
    }

    /** Just below and ahead of the gun hand, where the magazine goes in. */
    private static AABB magwellBox(InteractionHand gunHand) {
        VR.Aim gun = VrGunHand.aim(gunHand);
        return gun == null ? null : VrZones.magwell(gun, VR.worldScale());
    }

    /** The magazine held in the free hand. */
    private static AABB magazineBox(InteractionHand gunHand) {
        VR.Aim free = VrGunHand.aim(VrGunHand.other(gunHand));
        return free == null ? null : VrZones.magazine(free.pos(), VR.worldScale());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !VrClientConfig.showGuides()
                || !VR.isEyePass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !VR.active()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        if (holding) {
            boolean touching = magazineTouchesGun(heldGunHand);
            drawBox(pose, buffers, magazineBox(heldGunHand), 1.0F, 0.85F, 0.2F);
            if (touching) {
                drawBox(pose, buffers, magwellBox(heldGunHand), 0.2F, 1.0F, 0.3F);
            } else {
                drawBox(pose, buffers, magwellBox(heldGunHand), 1.0F, 1.0F, 1.0F);
            }
        } else {
            Pouch state = pouchState(player);
            InteractionHand gunHand = VrGunHand.of(player);
            if (gunHand == null) {
                state = Pouch.HIDDEN;
            }
            if (state == Pouch.NO_AMMO) {
                drawBox(pose, buffers, pouchBox(gunHand), 1.0F, 0.25F, 0.2F);
            } else if (state == Pouch.READY) {
                if (handInPouch(player)) {
                    drawBox(pose, buffers, pouchBox(gunHand), 0.2F, 1.0F, 0.3F);
                } else {
                    drawBox(pose, buffers, pouchBox(gunHand), 0.25F, 0.55F, 1.0F);
                }
            }
        }
        pose.popPose();
    }

    /** A see-through box with its outline, in world coordinates (the pose is already moved by -camera). */
    static void drawBox(PoseStack pose, MultiBufferSource.BufferSource buffers, AABB box, float r, float g, float b) {
        if (box == null) {
            return;
        }
        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        LevelRenderer.addChainedFilledBoxVertices(pose, fill, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                r, g, b, 0.2F);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, lines, box, r, g, b, 1.0F);
        // Not endBatch(type): with shaders it does nothing, and the box would be drawn later, in the other eye's view.
        buffers.endBatch();
    }
}
