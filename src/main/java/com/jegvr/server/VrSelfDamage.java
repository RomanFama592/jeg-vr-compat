package com.jegvr.server;

import com.jegvr.config.VrRules;
import com.jegvr.config.VrServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F9.8: a VR player's bullets may hit that same player (JEG always skips the shooter). Only for bullets that started at
 * the gun's muzzle ({@code ProjectileEntityVRMixin}): a bullet that starts inside the shooter's body never hits it,
 * because Minecraft's ray test only counts boxes the ray enters. Off unless the server's rule or, with the rule at
 * {@code PLAYER}, the player's own option ({@link com.jegvr.network.C2SVrSelfDamage}) turns it on.
 */
public final class VrSelfDamage {
    /** A self-inflicted headshot kills: more than any armour leaves standing (a totem still saves the player). */
    static final float LETHAL_DAMAGE = 1000.0F;

    private static final Map<UUID, Boolean> CHOICES = new ConcurrentHashMap<>();

    private VrSelfDamage() {
    }

    public static void choose(ServerPlayer player, boolean enabled) {
        CHOICES.put(player.getUUID(), enabled);
    }

    /** Whether a bullet {@code shooter} fires now from the muzzle may hit them. */
    public static boolean allows(LivingEntity shooter) {
        if (!(shooter instanceof ServerPlayer player)) {
            return false;
        }
        return VrRules.resolve(VrServerConfig.selfDamageRule(), CHOICES.getOrDefault(player.getUUID(), false));
    }

    /** The damage of a bullet that hit its own shooter: JEG's, or lethal for a headshot. */
    public static float damage(float jegDamage, boolean headshot) {
        return headshot ? Math.max(jegDamage, LETHAL_DAMAGE) : jegDamage;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHOICES.remove(event.getEntity().getUUID());
    }
}
