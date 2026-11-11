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
 * that keeps drawing and touching in agreement. The anchor is the first {@code Vec3.subtract} (HUD position minus
 * eye), the same in Vivecraft 1.2 and 1.3; 1.3 keeps the hotbar position in {@code GUI_RENDER_POS_ROOM}, also set after it.
 */
@Mixin(value = GuiHandler.class, remap = false)
public abstract class GuiHandlerVRMixin {
    @Inject(method = "applyGUIModelView", require = 0, at = @At(value = "INVOKE", remap = true, ordinal = 0,
            target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
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
