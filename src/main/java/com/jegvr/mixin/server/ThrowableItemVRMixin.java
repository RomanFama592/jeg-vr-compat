package com.jegvr.mixin.server;

import com.jegvr.server.VrServerThrow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.MolotovCocktailItem;
import ttv.migami.jeg.item.WaterBombItem;

/**
 * F9.26: JEG's thrown items (grenades, smoke and stun grenades inherit {@code GrenadeItem.releaseUsing}, molotov,
 * water bomb) leave from a VR player's throwing hand ({@link VrServerThrow}). Only with a fresh record from this
 * addon's client; everyone else throws as in JEG.
 */
@Mixin(value = {GrenadeItem.class, MolotovCocktailItem.class, WaterBombItem.class}, remap = false)
public abstract class ThrowableItemVRMixin {
    // releaseUsing overrides a vanilla method: that name in development, m_5551_ in the game (remap = false here).
    @WrapOperation(method = {"releaseUsing", "m_5551_"}, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jegvr$throwFromHand(Level level, Entity entity, Operation<Boolean> original,
                                        @Local(argsOnly = true) LivingEntity shooter) {
        VrServerThrow.aim(shooter, entity);
        return original.call(level, entity);
    }
}
