package com.jegvr.mixin.render;

import com.jegvr.client.VrGunGlow;
import com.jegvr.client.VrGunMirror;
import com.jegvr.client.VrHands;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vivecraft draws the held gun with {@code renderItem} right after its first-person transform ({@link
 * VivecraftItemRenderingVRMixin}). A mirrored gun ({@link VrGunMirror}) is drawn as a right-hand gun, through buffers
 * that undo the mirror's inside-out faces. F10.4: a gun that glows ({@link VrGunGlow}) is drawn through buffers that
 * copy its faces into the glowing layer, drawn right after it.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererVRMixin {
    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private ItemDisplayContext jegvr$mirroredContext(ItemDisplayContext context) {
        return VrGunMirror.pending() ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : context;
    }

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private boolean jegvr$mirroredLeftHand(boolean leftHand) {
        return !VrGunMirror.pending() && leftHand;
    }

    @ModifyVariable(method = "renderItem", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource jegvr$mirroredBuffers(MultiBufferSource buffers,
                                                    @Local(argsOnly = true) ItemStack stack) {
        MultiBufferSource drawn = VrGunMirror.pending() ? VrGunMirror.wrap(buffers) : buffers;
        return VrGunGlow.wrap(drawn, VrHands.renderingHand(), stack);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void jegvr$mirrorDrawn(CallbackInfo ci) {
        VrGunGlow.flush();
        VrGunMirror.drawn();
    }
}
