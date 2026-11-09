package com.jegvr.mixin.render;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

/** F10.4: see {@link CompositeRenderTypeAccessor}. */
@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface TextureStateShardAccessor {
    @Invoker("cutoutTexture")
    Optional<ResourceLocation> jegvr$cutoutTexture();
}
