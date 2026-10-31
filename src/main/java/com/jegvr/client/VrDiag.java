package com.jegvr.client;

import com.jegvr.JegVrCompat;
import com.jegvr.compat.VR;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/** One-shot diagnostics that tell us which JEG render path runs while the player is in VR. */
public final class VrDiag {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID + "/diag");
    private static final Set<String> SEEN = new HashSet<>();

    private VrDiag() {
    }

    public static void once(String key, String message, Object... args) {
        if (SEEN.add(key)) {
            LOGGER.info(message, args);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        if (VR.active()) {
            once("RenderHandEvent", "RenderHandEvent fires in VR (hand={}, canceled={})", event.getHand(), event.isCanceled());
        }
    }
}
