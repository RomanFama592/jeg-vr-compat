package com.jegvr.mixin.server;

import com.jegvr.server.VrOffhand;
import com.jegvr.server.VrSelfDamage;
import com.jegvr.server.VrServerAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.entity.projectile.ProjectileEntity;
import ttv.migami.jeg.item.GunItem;

/**
 * JEG places every new projectile at the shooter's eye. For VR players that told us where their gun is, start it at
 * the muzzle instead; the direction (player rotation from the shoot message, plus spread) is left to JEG.
 * <p>
 * F9.8: such a bullet may also hit its own shooter, when {@link VrSelfDamage#allows} says so. JEG skips the shooter
 * when looking for targets; everything after that (hit test, damage, effects) is JEG's own, with three exceptions for
 * the shooter only: the box is the current one (JEG rewinds other players' boxes by their ping), it does not grow
 * (JEG's {@code growBoundingBoxAmount}, meant to help aiming at others), and a headshot kills.
 */
@Mixin(value = ProjectileEntity.class, remap = false)
public abstract class ProjectileEntityVRMixin {
    @Shadow
    protected LivingEntity shooter;

    @Shadow
    private ItemStack weapon;

    /** F9.8: set at spawn; only for a bullet that started at a VR muzzle. */
    @Unique
    private boolean jegvr$hitsShooter;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lttv/migami/jeg/item/GunItem;Lttv/migami/jeg/common/Gun;)V",
            at = @At("RETURN"))
    private void jegvr$startAtMuzzle(EntityType<? extends Entity> type, Level level, LivingEntity shooter,
                                     ItemStack weapon, GunItem item, Gun modifiedGun, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
        InteractionHand hand = VrOffhand.firing() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Vec3 muzzle = VrServerAim.muzzleFor(shooter, hand);
        if (muzzle == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        self.setPos(muzzle);
        self.xo = muzzle.x;
        self.yo = muzzle.y;
        self.zo = muzzle.z;
        this.jegvr$hitsShooter = VrSelfDamage.allows(shooter);
    }

    /**
     * JEG decides some hit rules (the Holy Shotgun spares non-hostile targets) from the shooter's main-hand item. Read
     * the gun that fired instead, so a shot from the off hand follows that gun's rules. Same item in flat play.
     */
    @WrapOperation(method = "ignoreEntity", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jegvr$firingGun(LivingEntity shooter, Operation<ItemStack> original) {
        return weapon == null || weapon.isEmpty() ? original.call(shooter) : weapon;
    }

    /** F9.8: JEG's {@code entity.equals(shooter)} skip, in both target searches (single hit and piercing). */
    @WrapOperation(method = {"findEntityOnPath", "findEntitiesOnPath"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;equals(Ljava/lang/Object;)Z"))
    private boolean jegvr$shooterIsTarget(Entity candidate, Object other, Operation<Boolean> original) {
        if (this.jegvr$hitsShooter && candidate == this.shooter) {
            return false;
        }
        return original.call(candidate, other);
    }

    /** F9.8: the shooter's own box as it is now, not rewound by the shooter's ping. */
    @WrapOperation(method = "getHitResult", at = @At(value = "INVOKE",
            target = "Lttv/migami/jeg/common/BoundingBoxManager;getBoundingBox(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/phys/AABB;"))
    private AABB jegvr$shooterBoxNow(Player player, int ticksAgo, Operation<AABB> original) {
        return player == this.shooter ? player.getBoundingBox() : original.call(player, ticksAgo);
    }

    /** F9.8: the shooter's body and head boxes do not grow. */
    @WrapOperation(method = "getHitResult", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
    private AABB jegvr$shooterBoxNotGrown(AABB box, double x, double y, double z, Operation<AABB> original,
                                          @Local(argsOnly = true) Entity entity) {
        return entity == this.shooter ? box : original.call(box, x, y, z);
    }

    /** F9.8: a bullet that hit its own shooter in the head kills. */
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jegvr$selfHeadshot(Entity target, DamageSource source, float amount, Operation<Boolean> original,
                                       @Local(argsOnly = true) boolean headshot) {
        if (this.jegvr$hitsShooter && target == this.shooter) {
            amount = VrSelfDamage.damage(amount, headshot);
        }
        return original.call(target, source, amount);
    }
}
