package com.jegvr.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.common.ShootTracker;
import ttv.migami.jeg.common.network.ServerPlayHandler;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.message.C2SMessageShoot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Off-hand shot through JEG's own {@code ServerPlayHandler.handleShoot}. While {@link #firing()} is true,
 * {@code ServerPlayHandlerVRMixin} makes it read the off-hand gun and an off-hand fire-rate tracker, so ammo, rate,
 * spread, sounds and trails all come from JEG. Only loaded when JEG is present.
 */
public final class VrOffhandShot {
    /** JEG keys fire-rate cooldowns by item: the same gun in both hands needs its own tracker per hand. */
    private static final Map<UUID, ShootTracker> OFF_HAND_TRACKERS = new ConcurrentHashMap<>();

    private static boolean firing;

    private VrOffhandShot() {
    }

    public static void fire(ServerPlayer player, float yaw, float pitch) {
        if (!(player.getOffhandItem().getItem() instanceof GunItem) || firing) {
            return;
        }
        firing = true;
        try {
            ServerPlayHandler.handleShoot(new C2SMessageShoot(yaw, pitch), player);
        } finally {
            firing = false;
        }
    }

    /** True while an off-hand shot is being processed (server thread only). */
    public static boolean firing() {
        return firing;
    }

    public static ShootTracker tracker(Player player) {
        return OFF_HAND_TRACKERS.computeIfAbsent(player.getUUID(), id -> new ShootTracker());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OFF_HAND_TRACKERS.remove(event.getEntity().getUUID());
    }
}
