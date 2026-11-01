package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.mixin.input.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Reload by hand (phase 7): with the off hand free, grab a magazine at the hip with the off-hand grip, then bring it
 * to the main-hand gun. That starts JEG's normal reload (works on servers with only JEG). Guns loaded round by round
 * ({@code MANUAL}, shotguns) take one round per grab. JEG's reload key keeps working.
 * <p>
 * The off-hand grip is Vivecraft's "hotbar" key: a press inside the hip zone is taken from Vivecraft
 * ({@code KeyMappingVRMixin}), anywhere else it still changes the hotbar slot. {@link VrKeys#GRAB} is an optional
 * alternative for custom bindings.
 */
public final class VrGrabReload {
    /** Room-scale metres (multiplied by Vivecraft's world scale). */
    private static final double POUCH_DOWN = 0.65;
    private static final double POUCH_SIDE = 0.18;
    private static final double POUCH_FORWARD = 0.05;
    private static final double POUCH_RADIUS = 0.22;
    private static final double MAGWELL_FORWARD = 0.12;
    private static final double MAGWELL_DOWN = 0.08;
    private static final double MAGWELL_RADIUS = 0.15;
    private static final int TIMEOUT_TICKS = 100;

    private static final DustParticleOptions POUCH_DUST = new DustParticleOptions(new Vector3f(0.2F, 0.5F, 1.0F), 0.6F);
    private static final DustParticleOptions MAGWELL_DUST = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.3F), 0.6F);

    private static boolean holding;
    private static int heldTicks;
    private static int slot;
    private static boolean grabWasDown;
    /** MANUAL reload started by a grab: stop it once the gun has more rounds than this. */
    private static int singleRoundFrom = -1;
    private static boolean showZones;

    private VrGrabReload() {
    }

    /** {@code /jegvr zones}: show the hip and magazine-well zones as particles. */
    public static boolean toggleZones() {
        showZones = !showZones;
        return showZones;
    }

    /**
     * The off-hand grip was pressed (from {@code KeyMappingVRMixin}). Returns true when the press grabbed a magazine,
     * so Vivecraft must not also use it.
     */
    public static boolean onGripPressed() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (holding) {
            return true;
        }
        if (player == null || !canGrab(player)) {
            return false;
        }
        Vec3 pouch = pouch();
        VR.Aim hand = VR.offHandAim();
        if (pouch == null || hand == null || hand.pos().distanceTo(pouch) > POUCH_RADIUS * VR.worldScale()) {
            return false;
        }
        holding = true;
        heldTicks = 0;
        slot = player.getInventory().selected;
        VR.haptic(false, 0.05F, 0.6F);
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 0.6F, 1.3F);
        return true;
    }

    public static boolean isGripKey(KeyMapping key) {
        return key == VR.offHandGripKey();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        boolean grabDown = VrKeys.GRAB.isDown();
        if (grabDown && !grabWasDown) {
            onGripPressed();
        }
        grabWasDown = grabDown;
        if (player == null || !VR.active()) {
            holding = false;
            singleRoundFrom = -1;
            return;
        }
        tickSingleRound(player);
        if (holding) {
            tickHolding(player);
        }
        if (showZones && player.tickCount % 2 == 0) {
            showZones(player);
        }
    }

    private static void tickHolding(LocalPlayer player) {
        if (!gripDown() || ++heldTicks > TIMEOUT_TICKS || player.getInventory().selected != slot || !canGrab(player)) {
            holding = false;
            return;
        }
        Vec3 magwell = magwell();
        VR.Aim hand = VR.offHandAim();
        if (magwell == null || hand == null || hand.pos().distanceTo(magwell) > MAGWELL_RADIUS * VR.worldScale()) {
            return;
        }
        holding = false;
        ItemStack stack = player.getMainHandItem();
        GunItem gunItem = (GunItem) stack.getItem();
        if (ReloadType.MANUAL.equals(gunItem.getModifiedGun(stack).getReloads().getReloadType())) {
            singleRoundFrom = ammo(stack);
        }
        VrDualReload.reloadMainHand(player);
        VR.haptic(true, 0.08F, 0.8F);
        VR.haptic(false, 0.08F, 0.8F);
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

    private static boolean gripDown() {
        KeyMapping grip = VR.offHandGripKey();
        return VrKeys.GRAB.isDown() || (grip != null && ((KeyMappingAccessor) grip).jegvr$isDownRaw());
    }

    /** Off hand free, a main-hand gun that is reloaded (not inventory fed), not full, and ammo to load. */
    private static boolean canGrab(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!VR.active() || player.isSpectator() || Minecraft.getInstance().screen != null
                || !player.getOffhandItem().isEmpty() || !(stack.getItem() instanceof GunItem gunItem)
                || ModSyncedDataKeys.RELOADING.getValue(player)) {
            return false;
        }
        Gun gun = gunItem.getModifiedGun(stack);
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.getBoolean("IgnoreAmmo")
                || ReloadType.INVENTORY_FED.equals(gun.getReloads().getReloadType())
                || ammo(stack) >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return false;
        }
        return player.isCreative() || !Gun.findAmmo(player, gun.getReloads().getReloadItem()).stack().isEmpty();
    }

    private static int ammo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("AmmoCount");
    }

    /** Hip on the off-hand side, following the headset's yaw. */
    private static Vec3 pouch() {
        VR.Aim hmd = VR.hmdAim();
        if (hmd == null) {
            return null;
        }
        Vec3 forward = new Vec3(hmd.dir().x, 0.0, hmd.dir().z);
        if (forward.lengthSqr() < 1.0E-4) {
            return null;
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double side = VR.leftHanded() ? 1.0 : -1.0;
        double scale = VR.worldScale();
        return hmd.pos()
                .add(0.0, -POUCH_DOWN * scale, 0.0)
                .add(right.scale(POUCH_SIDE * side * scale))
                .add(forward.scale(POUCH_FORWARD * scale));
    }

    /** Just below and ahead of the main hand, where the magazine goes in. */
    private static Vec3 magwell() {
        VR.Aim main = VR.mainHandAim();
        if (main == null || main.dir().lengthSqr() < 1.0E-6) {
            return null;
        }
        double scale = VR.worldScale();
        return main.pos().add(main.dir().normalize().scale(MAGWELL_FORWARD * scale)).add(0.0, -MAGWELL_DOWN * scale, 0.0);
    }

    private static void showZones(LocalPlayer player) {
        Vec3 pouch = pouch();
        Vec3 magwell = magwell();
        if (pouch != null) {
            player.level().addParticle(POUCH_DUST, pouch.x, pouch.y, pouch.z, 0.0, 0.0, 0.0);
        }
        if (magwell != null) {
            player.level().addParticle(MAGWELL_DUST, magwell.x, magwell.y, magwell.z, 0.0, 0.0, 0.0);
        }
    }
}
