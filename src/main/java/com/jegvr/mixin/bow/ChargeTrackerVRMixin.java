package com.jegvr.mixin.bow;

import com.jegvr.client.VrBows;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.common.ChargeTracker;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageChargeSync;

import java.util.Map;
import java.util.UUID;

/**
 * F9.7: JEG charges a bow by how long its shoot key is held and sends the charge to the server every tick. With
 * Vivecraft's roomscale bow ({@link VrBows}) the charge is how far the string is pulled instead: overwrite JEG's value
 * and send ours right after it, so the server keeps ours.
 */
@Mixin(value = ChargeTracker.class, remap = false)
public abstract class ChargeTrackerVRMixin {
    @Shadow
    @Final
    private static Map<UUID, Float> lastChargeProgress;

    @Inject(method = "updateChargeTime", at = @At("TAIL"))
    private static void jegvr$drawCharge(Player player, ItemStack weapon, boolean charging, CallbackInfo ci) {
        if (!charging || !player.level().isClientSide() || player != Minecraft.getInstance().player || !VrBows.drawing()) {
            return;
        }
        float progress = VrBows.drawPercent();
        lastChargeProgress.put(player.getUUID(), progress);
        PacketHandler.getPlayChannel().sendToServer(new C2SMessageChargeSync(progress));
    }
}
