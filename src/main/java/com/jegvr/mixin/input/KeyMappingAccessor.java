package com.jegvr.mixin.input;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Raw pressed state, unaffected by {@link KeyMappingVRMixin} (which hides Vivecraft's teleport key). */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("isDown")
    boolean jegvr$isDownRaw();
}
