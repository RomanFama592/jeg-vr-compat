package com.jegvr.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F9.31: where each VR player's head looks, fed by {@link com.jegvr.network.C2SVrHead} while a stun grenade is near.
 * JEG blinds whoever "looks" at the grenade, and in VR the player's rotation follows the main controller, not the head.
 */
public final class VrServerHead {
    /** The client sends one every tick; a few may arrive late. */
    static final long MAX_AGE_TICKS = 5;

    private record Look(Vec3 direction, long gameTime) {
    }

    private static final Map<UUID, Look> LOOKS = new ConcurrentHashMap<>();

    private VrServerHead() {
    }

    public static void update(ServerPlayer player, Vec3 look) {
        Vec3 unit = unit(look);
        if (unit != null) {
            LOOKS.put(player.getUUID(), new Look(unit, player.level().getGameTime()));
        }
    }

    /** The direction the head of {@code entity} looks in, from a fresh record, or null to keep the entity's own. */
    public static Vec3 lookFor(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return null;
        }
        Look look = LOOKS.get(player.getUUID());
        return look != null && fresh(look.gameTime, player.level().getGameTime()) ? look.direction : null;
    }

    static boolean fresh(long recorded, long now) {
        return now >= recorded && now - recorded <= MAX_AGE_TICKS;
    }

    /** The direction as a unit vector, or null when it is not a direction (not finite, or no length). */
    static Vec3 unit(Vec3 look) {
        if (!VrServerAim.isFinite(look) || look.lengthSqr() < 1.0E-6) {
            return null;
        }
        return look.normalize();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOOKS.remove(event.getEntity().getUUID());
    }
}
