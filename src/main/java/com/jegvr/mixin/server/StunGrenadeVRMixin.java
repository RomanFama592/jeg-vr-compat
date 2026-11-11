package com.jegvr.mixin.server;

import com.jegvr.server.VrServerHead;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.entity.throwable.ThrowableStunGrenadeEntity;

/**
 * F9.31: a stun grenade blinds and deafens by the angle between where an entity looks and the grenade. In VR Vivecraft
 * turns the player towards the main controller's crosshair, so JEG measured where the hand points. With a fresh record
 * from this addon's client ({@link VrServerHead}), it is where the head looks. Everyone else is left as JEG has them.
 */
@Mixin(value = ThrowableStunGrenadeEntity.class, remap = false)
public abstract class StunGrenadeVRMixin {
    @WrapOperation(method = "onDeath", require = 0, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/LivingEntity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 jegvr$lookWithTheHead(LivingEntity entity, float partialTick, Operation<Vec3> original) {
        Vec3 head = VrServerHead.lookFor(entity);
        return head != null ? head : original.call(entity, partialTick);
    }
}
