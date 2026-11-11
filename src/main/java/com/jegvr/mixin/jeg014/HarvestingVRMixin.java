package com.jegvr.mixin.jeg014;

import com.jegvr.server.VrOffhand;
import com.jegvr.server.VrServerAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.common.ShootTracker;
import ttv.migami.jeg.common.network.ServerPlayHandler;
import ttv.migami.jeg.network.message.C2SMessageShoot;

/**
 * JEG 0.14 only (the plugin skips it with an older JEG, which has no such method). The chainsaw and the jackhammer
 * "shoot" through {@code handleHarvesting}: a 4-block reach from the player's eyes for mobs and for the block, always
 * with the main-hand tool. For a VR player whose gun position the server knows ({@link VrServerAim}) the reach starts at
 * the tool and follows it; and during a {@link VrOffhand} shot it is the off-hand tool, with that hand's fire rate.
 * Players in flat, and VR players on a server that was not told where their tool is, are left as JEG has them.
 */
@Mixin(value = ServerPlayHandler.class, remap = false)
public abstract class HarvestingVRMixin {
    private static final String HARVESTING = "handleHarvesting";

    /**
     * JEG turns the player to the shot's rotation only for the jackhammer, and for the chainsaw only after both reaches
     * were traced (so those follow the head). With a known tool position, the shot's rotation applies from the start.
     */
    @Inject(method = HARVESTING, at = @At("HEAD"), require = 0)
    private static void jegvr$aimWithTheTool(C2SMessageShoot message, ServerPlayer player, CallbackInfo ci) {
        if (VrServerAim.muzzleFor(player, VrOffhand.firingHand()) != null) {
            player.setYRot(Mth.wrapDegrees(message.getRotationYaw()));
            player.setXRot(Mth.clamp(message.getRotationPitch(), -90.0F, 90.0F));
        }
    }

    @WrapOperation(method = HARVESTING, require = 0, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/server/level/ServerPlayer;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 jegvr$reachFromTheTool(ServerPlayer player, Operation<Vec3> original) {
        Vec3 muzzle = VrServerAim.muzzleFor(player, VrOffhand.firingHand());
        return muzzle != null ? muzzle : original.call(player);
    }

    /** {@code Entity.pick} from the tool instead of the eyes: same reach, same rules. */
    @WrapOperation(method = HARVESTING, require = 0, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/server/level/ServerPlayer;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult jegvr$mineFromTheTool(ServerPlayer player, double reach, float partialTick, boolean fluids,
                                                   Operation<HitResult> original) {
        Vec3 muzzle = VrServerAim.muzzleFor(player, VrOffhand.firingHand());
        if (muzzle == null) {
            return original.call(player, reach, partialTick, fluids);
        }
        Vec3 end = muzzle.add(player.getViewVector(partialTick).scale(reach));
        return player.level().clip(new ClipContext(muzzle, end, ClipContext.Block.OUTLINE,
                fluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
    }

    @WrapOperation(method = HARVESTING, require = 0, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/server/level/ServerPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jegvr$offhandTool(ServerPlayer player, Operation<ItemStack> original) {
        return VrOffhand.firing() ? player.getOffhandItem() : original.call(player);
    }

    @WrapOperation(method = HARVESTING, require = 0, at = @At(value = "INVOKE",
            target = "Lttv/migami/jeg/common/ShootTracker;getShootTracker(Lnet/minecraft/world/entity/player/Player;)Lttv/migami/jeg/common/ShootTracker;"))
    private static ShootTracker jegvr$offhandTracker(Player player, Operation<ShootTracker> original) {
        return VrOffhand.firing() ? VrOffhand.tracker(player) : original.call(player);
    }
}
