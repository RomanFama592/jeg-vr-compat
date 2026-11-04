package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.util.GunModifierHelper;

/** JEG's melee damage and knockback read the bayonet of the main-hand gun; during an off-hand melee, of the off-hand gun. */
@Mixin(value = GunModifierHelper.class, remap = false)
public abstract class GunModifierHelperVRMixin {
    @WrapOperation(method = {"getSwordDamage", "getSwordKnockBack", "getSwordFireAspect", "getSwordSweepingEdge"},
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$firingGun(Player player, Operation<ItemStack> original) {
        // firing() is only set on the server thread; in single player the client thread must not see it.
        return VrOffhand.firing() && !player.level().isClientSide ? player.getOffhandItem() : original.call(player);
    }
}
