package com.jegvr.server;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.AmmoContext;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunModifierHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hand loading (F7.8): a VR player pushed one round into a gun loaded round by round ({@code ReloadType.MANUAL}). Loads
 * that round at once, the way JEG's {@code ReloadTracker.increaseAmmo} does for each round of its own reload, without
 * JEG's per-round wait or reload animation. Only the main hand for now (the off hand comes with ambidextrous play).
 */
public final class VrInsertRound {
    /** Fewest ticks between two rounds, so it is only faster than JEG for a quick player. */
    private static final int MIN_GAP_TICKS = 3;

    private static final Map<UUID, Long> LAST_ROUND = new ConcurrentHashMap<>();

    private VrInsertRound() {
    }

    public static void insert(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !(stack.getItem() instanceof GunItem gunItem)
                || ModSyncedDataKeys.RELOADING.getValue(player)) {
            return;
        }
        Gun gun = gunItem.getModifiedGun(stack);
        CompoundTag tag = stack.getOrCreateTag();
        int ammo = tag.getInt("AmmoCount");
        if (!ReloadType.MANUAL.equals(gun.getReloads().getReloadType()) || tag.getBoolean("IgnoreAmmo")
                || ammo >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_ROUND.get(player.getUUID());
        if (last != null && now - last < Math.max(MIN_GAP_TICKS, gun.getReloads().getReloadTimer() / 2)) {
            return;
        }
        if (!player.isCreative()) {
            // The item JEG loads into MANUAL guns is the round itself (increaseAmmo looks up the projectile item).
            AmmoContext context = Gun.findAmmo(player, gun.getProjectile().getItem());
            if (context.stack().isEmpty()) {
                return;
            }
            context.stack().shrink(1);
            if (context.container() != null) {
                context.container().setChanged();
            }
        }
        LAST_ROUND.put(player.getUUID(), now);
        tag.putInt("AmmoCount", ammo + 1);
        playSound(player, gun.getSounds().getReloadLoad());
        if (ammo == 0) {
            // An empty gun also gets JEG's closing sound on its first round, without JEG's extra wait.
            playSound(player, gun.getSounds().getReloadEnd());
        }
    }

    private static void playSound(ServerPlayer player, ResourceLocation id) {
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS,
                    1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ROUND.remove(event.getEntity().getUUID());
    }
}
