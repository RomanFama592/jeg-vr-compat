package com.jegvr;

import com.jegvr.compat.VR;
import com.jegvr.client.VrDebugCommands;
import com.jegvr.client.VrDiag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(JegVrCompat.MOD_ID)
public class JegVrCompat {
    public static final String MOD_ID = "jeg_vr_compat";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public JegVrCompat() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Client-only classes: register here, never in the constructor (dedicated servers also load this mod).
        MinecraftForge.EVENT_BUS.register(VrDiag.class);
        MinecraftForge.EVENT_BUS.register(VrDebugCommands.class);
        VR.preload();
        LOGGER.info("Vivecraft present: {}, VR active at startup: {}", VR.loaded(), VR.active());
    }
}
