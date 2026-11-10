package com.jegvr.mixin.render;

import com.jegvr.client.VrGunHud;
import com.jegvr.client.VrGripHint;
import com.jegvr.client.VrGunMirror;
import com.jegvr.client.VrHands;
import com.jegvr.client.VrHandFollow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.helpers.VRArmHelper;

/**
 * F9.9: the panels beside the hands are drawn after the hands, so the guns cannot cover them ({@link VrGunHud}).
 * Phase H: the hands let go of the controllers while an animation plays ({@link VrHandFollow}).
 * F10.4: the ghost hand showing how to hold a two-handed gun ({@link VrGripHint}).
 */
@Mixin(value = VRArmHelper.class, remap = false)
public abstract class VRArmHelperVRMixin {
    private static final String RENDER_ARM_WITH_ITEM = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String SETUP_AT_CONTROLLER = "Lorg/vivecraft/client_vr/render/helpers/RenderHelper;setupRenderingAtController(ILcom/mojang/blaze3d/vertex/PoseStack;)V";

    @Inject(method = "renderVRHands", at = @At("HEAD"))
    private static void jegvr$beginHands(float partialTick, boolean renderMain, boolean renderOff, boolean menuHandMain,
                                         boolean menuHandOff, PoseStack poseStack, CallbackInfo ci) {
        if ((renderMain || renderOff) && !menuHandMain && !menuHandOff) {
            VrHandFollow.beginHands();
        }
    }

    // Before the nausea wobble returns, which the hands were measured without.
    @Inject(method = "renderVRHands", at = @At(value = "INVOKE",
            target = "Lorg/vivecraft/client_vr/render/helpers/VREffectsHelper;reAddNausea(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private static void jegvr$drawFollowingHands(float partialTick, boolean renderMain, boolean renderOff,
                                                 boolean menuHandMain, boolean menuHandOff, PoseStack poseStack,
                                                 CallbackInfo ci) {
        if (!menuHandMain && !menuHandOff) {
            VrHandFollow.drawFollowing(poseStack);
            VrGripHint.drawGhost(poseStack);
        }
    }

    @WrapOperation(method = "renderVRHand_Main", at = @At(value = "INVOKE", target = SETUP_AT_CONTROLLER))
    private static void jegvr$heavyMainHand(int controller, PoseStack poseStack, Operation<Void> original) {
        original.call(controller, poseStack);
        VrHandFollow.applyHeavy(poseStack, true);
    }

    // Ordinal 0 is the hand; the next one is the teleport energy bar.
    @WrapOperation(method = "renderVRHand_Offhand", at = @At(value = "INVOKE", target = SETUP_AT_CONTROLLER, ordinal = 0))
    private static void jegvr$heavyOffHand(int controller, PoseStack poseStack, Operation<Void> original) {
        original.call(controller, poseStack);
        VrHandFollow.applyHeavy(poseStack, false);
    }

    @ModifyArg(method = "renderVRHand_Main", index = 5,
            at = @At(value = "INVOKE", target = RENDER_ARM_WITH_ITEM, remap = true))
    private static ItemStack jegvr$mainHandItem(ItemStack stack) {
        return VrHandFollow.heldItem(stack, true);
    }

    @ModifyArg(method = "renderVRHand_Offhand", index = 5,
            at = @At(value = "INVOKE", target = RENDER_ARM_WITH_ITEM, remap = true))
    private static ItemStack jegvr$offHandItem(ItemStack stack) {
        return VrHandFollow.heldItem(stack, false);
    }

    @Inject(method = "renderVRHands", at = @At("RETURN"))
    private static void jegvr$panelsOverGuns(float partialTick, boolean renderMain, boolean renderOff, boolean menuHandMain,
                                             boolean menuHandOff, PoseStack poseStack, CallbackInfo ci) {
        if ((renderMain || renderOff) && !menuHandMain && !menuHandOff) {
            VrGunHud.render(poseStack);
        }
        // What is drawn from here on (a shader pack's shadows, the third-person camera, the hotbar) is no hand's gun.
        VrHands.setRenderingHand(null);
        VrGunMirror.drawn();
    }
}
