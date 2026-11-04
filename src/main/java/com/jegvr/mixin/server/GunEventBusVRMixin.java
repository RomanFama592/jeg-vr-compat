package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.jegvr.server.VrServerAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.event.GunEventBus;

/**
 * JEG's shot listeners read the gun from the shooter's main hand, not from the event: the pre-shot checks, durability,
 * casings and the special shots (the sonic blast of the Supersonic Shotgun and Hypersonic Cannon is fired here, not as
 * a projectile). While the server processes an off-hand shot ({@link VrOffhand#firing()}), give them the off-hand gun,
 * so each hand fires its own gun. Outside off-hand shots nothing changes.
 */
@Mixin(value = GunEventBus.class, remap = false)
public abstract class GunEventBusVRMixin {
    @WrapOperation(method = {"preShoot", "postShoot"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$firingGun(Player player, Operation<ItemStack> original) {
        return firingHand(player, original.call(player));
    }

    @WrapOperation(method = {"ejectCasing", "specialEffects"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$firingGunEffects(LivingEntity entity, Operation<ItemStack> original) {
        return firingHand(entity, original.call(entity));
    }

    /**
     * The Typhoonee's blast looks for its target with {@code pick(64, 0, false)}, which at partial tick 0 reads last
     * tick's rotation, not the one the shoot message just set: in VR that is where the main hand pointed. For a VR shot,
     * it reads the rotation of the shot. Flat players (no fresh record in {@link VrServerAim}) are untouched.
     */
    @WrapOperation(method = "typhooneeBlast", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/LivingEntity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult jegvr$pickAlongShot(LivingEntity shooter, double distance, float partialTick, boolean fluids,
                                                 Operation<HitResult> original) {
        if (!VrOffhand.firing() && VrServerAim.muzzleFor(shooter, InteractionHand.MAIN_HAND) == null) {
            return original.call(shooter, distance, partialTick, fluids);
        }
        float xRotO = shooter.xRotO;
        float yHeadRotO = shooter.yHeadRotO;
        shooter.xRotO = shooter.getXRot();
        shooter.yHeadRotO = shooter.getYRot();
        try {
            return original.call(shooter, distance, partialTick, fluids);
        } finally {
            shooter.xRotO = xRotO;
            shooter.yHeadRotO = yHeadRotO;
        }
    }

    private static ItemStack firingHand(LivingEntity entity, ItemStack mainHand) {
        // firing() is only set on the server thread; in single player the client thread must not see it.
        return VrOffhand.firing() && !entity.level().isClientSide ? entity.getOffhandItem() : mainHand;
    }
}
