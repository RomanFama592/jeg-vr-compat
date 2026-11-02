package com.jegvr.mixin.render;

import com.jegvr.client.VrHud;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;

/**
 * F9.14: Vivecraft can lock its HUD (with the hotbar) to the off-hand controller ({@code HUDLock.HAND} or {@code WRIST});
 * a gun in that hand then covers it or swallows it. {@code GuiHandler.applyGUIModelView} computes the HUD position
 * ({@code guipos}), then uses it both to draw the HUD and, through {@code GUI_POS_WORLD}, to find the hotbar slot the
 * other hand touches. Changing {@code guipos}, the rotation {@code guirot} and the offset {@code guilocal} right before
 * that keeps drawing and touching in agreement.
 */
@Mixin(value = GuiHandler.class, remap = false)
public abstract class GuiHandlerVRMixin {
    @Inject(method = "applyGUIModelView", at = @At(value = "INVOKE",
            target = "Lorg/vivecraft/client_vr/render/helpers/RenderHelper;getSmoothCameraPosition(Lorg/vivecraft/client_vr/render/RenderPass;Lorg/vivecraft/client_vr/VRData;)Lnet/minecraft/world/phys/Vec3;"))
    private static void jegvr$hudOnForearm(CallbackInfoReturnable<Vec3> cir, @Local(name = "guipos") LocalRef<Vec3> guipos,
                                           @Local(name = "guirot") Matrix4f guirot,
                                           @Local(name = "guilocal") Vector3f guilocal,
                                           @Local(name = "scale") float scale) {
        Vec3 pos = guipos.get();
        if (pos != null && guirot != null && guilocal != null) {
            guipos.set(VrHud.place(pos, guirot, guilocal, scale));
        }
    }
}
