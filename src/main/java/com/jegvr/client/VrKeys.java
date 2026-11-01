package com.jegvr.client;

import com.jegvr.JegVrCompat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

/** Addon key mappings. No default keys: in VR they are bound to controller buttons through Vivecraft/SteamVR. */
public final class VrKeys {
    public static final String CATEGORY = "key.categories." + JegVrCompat.MOD_ID;

    /** Fires the gun held in the off hand (dual wield; needs the addon on the server). */
    public static final KeyMapping FIRE_OFFHAND = new KeyMapping("key." + JegVrCompat.MOD_ID + ".fire_offhand",
            InputConstants.UNKNOWN.getValue(), CATEGORY);

    /** Grab (reload by hand, F7). Optional: the off-hand grip already works with Vivecraft's default bindings. */
    public static final KeyMapping GRAB = new KeyMapping("key." + JegVrCompat.MOD_ID + ".grab",
            InputConstants.UNKNOWN.getValue(), CATEGORY);

    private VrKeys() {
    }

    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(FIRE_OFFHAND);
        event.register(GRAB);
    }
}
