package com.jegvr.mixin.model;

import com.jegvr.client.VrDiag;
import com.jegvr.compat.VR;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.item.GunItem;

import java.lang.reflect.Method;

/**
 * Third person: JEG's {@code ItemInHandLayerMixin} never draws a gun held in the off hand (it cancels the layer). A VR
 * player can fire a gun from each hand, so for them draw the off-hand gun the way JEG draws the main-hand one, with
 * JEG's own {@code mrCrayfishGunMod$renderArmWithGun} (merged into this class by JEG's mixin). Priority 900 so this
 * runs before JEG's HEAD callback. Non-VR players are left to JEG.
 */
@Mixin(value = ItemInHandLayer.class, priority = 900)
public abstract class ItemInHandLayerVRMixin {
    @Unique
    private static Method jegvr$renderArmWithGun;
    @Unique
    private static boolean jegvr$lookedUp;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void jegvr$drawOffhandGun(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                      PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (!(entity instanceof Player player) || !(stack.getItem() instanceof GunItem gunItem)
                || !((Object) this instanceof PlayerItemInHandLayer<?, ?> layer)) {
            return;
        }
        InteractionHand hand = arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (hand != InteractionHand.OFF_HAND || !VR.isVRPlayer(player)) {
            return;
        }
        Method render = jegvr$renderMethod();
        if (render == null) {
            return;
        }
        try {
            render.invoke(null, layer, player, stack, gunItem, context, hand, arm, poseStack, buffer, light,
                    Minecraft.getInstance().getFrameTime());
            ci.cancel();
        } catch (ReflectiveOperationException | RuntimeException e) {
            VrDiag.once("offhandGunLayer", "Cannot draw the off-hand gun in third person: {}", e.toString());
            jegvr$renderArmWithGun = null;
        }
    }

    @Unique
    private static Method jegvr$renderMethod() {
        if (!jegvr$lookedUp) {
            jegvr$lookedUp = true;
            try {
                Method method = ItemInHandLayer.class.getDeclaredMethod("mrCrayfishGunMod$renderArmWithGun",
                        PlayerItemInHandLayer.class, Player.class, ItemStack.class, GunItem.class,
                        ItemDisplayContext.class, InteractionHand.class, HumanoidArm.class, PoseStack.class,
                        MultiBufferSource.class, int.class, float.class);
                method.setAccessible(true);
                jegvr$renderArmWithGun = method;
            } catch (ReflectiveOperationException | RuntimeException e) {
                VrDiag.once("offhandGunLayer", "JEG's third-person gun renderer not found: {}", e.toString());
            }
        }
        return jegvr$renderArmWithGun;
    }
}
