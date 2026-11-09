package com.jegvr.mixin.render;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** F10.4: the texture a gun is drawn with, to tint it ({@link com.jegvr.client.VrGunGlow}). */
@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface CompositeRenderTypeAccessor {
    @Invoker("state")
    RenderType.CompositeState jegvr$state();
}
