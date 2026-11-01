package com.jegvr.mixin.server;

import com.jegvr.server.VrServerAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.item.GunItem;

/**
 * JEG places every new projectile at the shooter's eye. For VR players that told us where their gun is, start it at
 * the muzzle instead; the direction (player rotation from the shoot message, plus spread) is left to JEG.
 */
@Mixin(value = ProjectileEntity.class, remap = false)
public abstract class ProjectileEntityVRMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lttv/migami/jeg/item/GunItem;Lttv/migami/jeg/common/Gun;)V",
            at = @At("RETURN"))
    private void jegvr$startAtMuzzle(EntityType<? extends Entity> type, Level level, LivingEntity shooter,
                                     ItemStack weapon, GunItem item, Gun modifiedGun, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        Vec3 muzzle = VrServerAim.muzzleFor(shooter);
        if (muzzle == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        self.setPos(muzzle);
        self.xo = muzzle.x;
        self.yo = muzzle.y;
        self.zo = muzzle.z;
    }
}
