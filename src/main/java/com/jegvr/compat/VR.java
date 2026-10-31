package com.jegvr.compat;

import com.jegvr.JegVrCompat;
import com.jegvr.compat.vivecraft.VivecraftBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Safe facade: every method is false/null when Vivecraft is absent or its API misbehaves. */
public final class VR {
    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static Boolean loaded;
    private static boolean broken;

    private VR() {
    }

    public static boolean loaded() {
        if (loaded == null) {
            ModList list = ModList.get();
            if (list == null) {
                return false;
            }
            loaded = list.isLoaded("vivecraft");
        }
        return loaded && !broken;
    }

    /** True when the local player is currently playing in VR. */
    public static boolean active() {
        if (!loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isActive();
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    public static boolean isVRPlayer(Player player) {
        if (player == null || !loaded()) {
            return false;
        }
        try {
            return VivecraftBridge.isVRPlayer(player);
        } catch (Throwable t) {
            disable(t);
            return false;
        }
    }

    private static void disable(Throwable t) {
        broken = true;
        LOGGER.error("Vivecraft bridge failed, VR compat disabled for this session", t);
    }
}
