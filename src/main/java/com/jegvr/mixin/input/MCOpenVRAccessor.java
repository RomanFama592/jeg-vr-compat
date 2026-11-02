package com.jegvr.mixin.input;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.vivecraft.client_vr.provider.ControllerType;
import org.vivecraft.client_vr.provider.openvr_lwjgl.MCOpenVR;

/** Which physical controller (left or right) a SteamVR input origin belongs to. Used only by VivecraftBridge. */
@Mixin(value = MCOpenVR.class, remap = false)
public interface MCOpenVRAccessor {
    @Invoker("getOriginControllerType")
    ControllerType jegvr$originController(long origin);
}
