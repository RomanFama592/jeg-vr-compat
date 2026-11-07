package com.jegvr.server;

import com.jegvr.compat.jeg.JegKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ShootTracker;
import ttv.migami.jeg.common.network.ServerPlayHandler;
import ttv.migami.jeg.event.FlashlightEvent;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.network.message.C2SMessagePreFireSound;
import ttv.migami.jeg.network.message.C2SMessageShoot;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The off-hand gun (dual wield), run through JEG's own server code:
 * <ul>
 * <li>Shots: {@code ServerPlayHandler.handleShoot} while {@link #firing()} is true; {@code ServerPlayHandlerVRMixin}
 * makes it read the off-hand gun and an off-hand fire-rate tracker.</li>
 * <li>Reloads: JEG's {@code ReloadTracker} while {@link #reloading(Player)} is true; {@code ReloadTrackerVRMixin}
 * makes it reload the off-hand gun.</li>
 * </ul>
 * Ammo, rate, spread, sounds and trails all come from JEG. Only loaded when JEG is present.
 */
public final class VrOffhand {
    /** JEG keys fire-rate cooldowns by item: the same gun in both hands needs its own tracker per hand. */
    private static final Map<UUID, ShootTracker> OFF_HAND_TRACKERS = new ConcurrentHashMap<>();
    private static final Set<UUID> RELOADING = ConcurrentHashMap.newKeySet();

    private static boolean firing;
    private static boolean attachmentsPass;
    private static boolean clientMenuForOffhand;

    private VrOffhand() {
    }

    /** Guns that can be used from the off hand. */
    public static boolean canDualWield(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    /**
     * JEG's projectile reads one charge per player (synced by {@code C2SMessageChargeSync} for the main hand). For this
     * shot it reads the off-hand gun's; the main hand's goes back right after, even in the middle of charging.
     */
    public static void fire(ServerPlayer player, float yaw, float pitch, float charge) {
        if (!canDualWield(player.getOffhandItem()) || firing || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                || !Float.isFinite(charge)) {
            return;
        }
        firing = true;
        CompoundTag data = player.getPersistentData();
        float mainCharge = data.getFloat(JegKeys.CHARGE_PROGRESS);
        data.putFloat(JegKeys.CHARGE_PROGRESS, Mth.clamp(charge, 0.0F, 1.0F));
        try {
            ServerPlayHandler.handleShoot(new C2SMessageShoot(yaw, pitch), player);
        } finally {
            data.putFloat(JegKeys.CHARGE_PROGRESS, mainCharge);
            firing = false;
        }
    }

    /** The off-hand gun's wind-up sound, through JEG's handler. */
    public static void preFireSound(ServerPlayer player) {
        if (!canDualWield(player.getOffhandItem()) || firing) {
            return;
        }
        firing = true;
        try {
            ServerPlayHandler.handlePreFireSound(new C2SMessagePreFireSound(), player);
        } finally {
            firing = false;
        }
    }

    /** JEG's melee attack with the off-hand gun, with the player turned towards the target just for it. */
    public static void melee(ServerPlayer player, float yaw, float pitch) {
        if (!canDualWield(player.getOffhandItem()) || firing || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return;
        }
        firing = true;
        float currentYaw = player.getYRot();
        float currentPitch = player.getXRot();
        player.setYRot(Mth.wrapDegrees(yaw));
        player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        try {
            ServerPlayHandler.handleMelee(player);
        } finally {
            player.setYRot(currentYaw);
            player.setXRot(currentPitch);
            firing = false;
        }
    }

    /** JEG's unload of the off-hand gun: gives its ammo back. */
    public static void unload(ServerPlayer player) {
        runOnOffhandGun(player, () -> ServerPlayHandler.handleUnload(player));
    }

    /** JEG's attachments screen, on the off-hand gun. */
    public static void openAttachments(ServerPlayer player) {
        runOnOffhandGun(player, () -> ServerPlayHandler.handleAttachments(player));
    }

    private static void runOnOffhandGun(ServerPlayer player, Runnable jegHandler) {
        if (!canDualWield(player.getOffhandItem()) || firing) {
            return;
        }
        firing = true;
        try {
            jegHandler.run();
        } finally {
            firing = false;
        }
    }

    /** The client asked for the attachments screen of its off-hand gun; it stays so until that screen closes. */
    public static void setClientMenuForOffhand(boolean offhand) {
        clientMenuForOffhand = offhand;
    }

    public static boolean clientMenuForOffhand() {
        return clientMenuForOffhand;
    }

    /** Whether the attachments menu being built for {@code player} is for the off-hand gun, on either side. */
    public static boolean menuForOffhand(Player player) {
        return player.level().isClientSide ? clientMenuForOffhand : firing;
    }

    /** True while JEG's server code for one action is running on the off-hand gun (server thread only). */
    public static boolean firing() {
        return firing;
    }

    public static ShootTracker tracker(Player player) {
        return OFF_HAND_TRACKERS.computeIfAbsent(player.getUUID(), id -> new ShootTracker());
    }

    /** Starts a JEG reload of the off-hand gun; it ends when JEG clears its RELOADING flag. */
    public static void startReload(ServerPlayer player) {
        ItemStack stack = player.getOffhandItem();
        if (player.isSpectator() || !canDualWield(stack) || ModSyncedDataKeys.RELOADING.getValue(player)) {
            return;
        }
        Gun gun = ((GunItem) stack.getItem()).getModifiedGun(stack);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getInt(JegKeys.AMMO_COUNT) >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return;
        }
        RELOADING.add(player.getUUID());
        ModSyncedDataKeys.RELOADING.setValue(player, true);
        // Drives the off-hand gun's reload animation; set here too so inventory sync does not wipe the client's copy.
        stack.getOrCreateTag().putBoolean(JegKeys.IS_RELOADING, true);
    }

    /** True while {@code player}'s current JEG reload is for the off-hand gun. */
    public static boolean reloading(Player player) {
        return !RELOADING.isEmpty() && RELOADING.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        boolean jegReloading = ModSyncedDataKeys.RELOADING.getValue(player);
        if (!player.level().isClientSide && reloading(player) && !jegReloading) {
            RELOADING.remove(player.getUUID());
        }
        ItemStack offhand = player.getOffhandItem();
        tickAttachments(event, player, offhand);
        tickShootAnimation(offhand);
        CompoundTag tag = offhand.getTag();
        if (!jegReloading && tag != null && tag.getBoolean(JegKeys.IS_RELOADING) && offhand.getItem() instanceof GunItem) {
            tag.remove(JegKeys.IS_RELOADING);
        }
    }

    /**
     * JEG runs the flashlight and the laser pointer of the main-hand gun every tick. Run them for the off-hand gun too,
     * while the client reports where that gun points: without that record the player is not in VR with this addon.
     */
    private static void tickAttachments(TickEvent.PlayerTickEvent event, Player player, ItemStack offhand) {
        if (player.level().isClientSide || !(offhand.getItem() instanceof GunItem)
                || !Gun.hasAttachmentEquipped(offhand, IAttachment.Type.SPECIAL)
                || VrServerAim.muzzleFor(player, InteractionHand.OFF_HAND) == null
                || VrServerAim.directionFor(player, InteractionHand.OFF_HAND) == null) {
            return;
        }
        attachmentsPass = true;
        try {
            FlashlightEvent.laserPointer(event);
            FlashlightEvent.flashlight(event);
        } finally {
            attachmentsPass = false;
        }
    }

    /**
     * True while JEG's flashlight and laser pointer run for the off-hand gun. Only set on the server thread; JEG stops
     * before any of the calls that read it when the player is on the client side.
     */
    public static boolean attachmentsPass() {
        return attachmentsPass;
    }

    /**
     * JEG only advances the shoot animation of the main-hand gun; do the same for the off hand, or its "IsShooting"
     * tag would never clear. Flat JEG never sets that tag on the off hand, so this is a no-op there.
     */
    private static void tickShootAnimation(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (!(stack.getItem() instanceof AnimatedGunItem) || tag == null || !tag.getBoolean(JegKeys.IS_SHOOTING)) {
            return;
        }
        int tick = tag.getInt(JegKeys.ANIMATION_TICK) + 1;
        if (tick >= JegKeys.SHOOT_ANIMATION_TICKS) {
            tag.remove(JegKeys.IS_SHOOTING);
            tag.remove(JegKeys.ANIMATION_TICK);
        } else {
            tag.putInt(JegKeys.ANIMATION_TICK, tick);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OFF_HAND_TRACKERS.remove(event.getEntity().getUUID());
        RELOADING.remove(event.getEntity().getUUID());
    }
}
