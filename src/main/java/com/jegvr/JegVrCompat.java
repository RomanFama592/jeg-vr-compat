package com.jegvr;

import com.jegvr.compat.VR;
import com.jegvr.compat.Versions;
import com.jegvr.config.VrClientConfig;
import com.jegvr.config.VrServerConfig;
import com.jegvr.client.VrRulesClient;
import com.jegvr.client.VrConfigScreen;
import com.jegvr.client.VrDebugCommands;
import com.jegvr.client.VrGunConfig;
import com.jegvr.client.VrDiag;
import com.jegvr.client.VrDualReload;
import com.jegvr.client.VrBowLaser;
import com.jegvr.client.VrGrabReload;
import com.jegvr.client.VrKeys;
import com.jegvr.client.VrOffhandShooting;
import com.jegvr.client.VrGripHint;
import com.jegvr.client.VrPrecision;
import com.jegvr.client.VrThrow;
import com.jegvr.client.VrRecoil;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrInsertRound;
import com.jegvr.server.VrOffhand;
import com.jegvr.server.VrServerAim;
import com.jegvr.server.VrServerRules;
import com.jegvr.server.VrServerThrow;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

@Mod(JegVrCompat.MOD_ID)
public class JegVrCompat {
    public static final String MOD_ID = "jeg_vr_compat";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public JegVrCompat() {
        // What decides which mixins and which VR bridge this session uses: the first thing to read in a bug report.
        LOGGER.info("Detected {}", Versions.profile());
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onLoadComplete);
        if (FMLEnvironment.dist.isClient()) {
            VrClientConfig.register();
            VrConfigScreen.register();
            modBus.addListener(VrKeys::onRegister);
        }
        // Common to both sides: the optional server component (phase S).
        VrServerConfig.register();
        modBus.addListener(VrServerRules::onConfigReload);
        VrNetwork.init();
        MinecraftForge.EVENT_BUS.register(VrServerRules.class);
        MinecraftForge.EVENT_BUS.register(VrServerAim.class);
        MinecraftForge.EVENT_BUS.register(VrServerThrow.class);
        if (ModList.get().isLoaded("jeg")) {
            MinecraftForge.EVENT_BUS.register(VrOffhand.class);
            MinecraftForge.EVENT_BUS.register(VrInsertRound.class);
        }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Client-only classes: register here, never in the constructor (dedicated servers also load this mod).
        MinecraftForge.EVENT_BUS.register(VrDiag.class);
        MinecraftForge.EVENT_BUS.register(VrRulesClient.class);
        // Read the gun poses now (built-in file, then the user's), so a broken file shows up in the log at startup.
        VrGunConfig.load();
        if (ModList.get().isLoaded("jeg")) {
            // These classes use JEG types: only register them when JEG is present.
            MinecraftForge.EVENT_BUS.register(VrDebugCommands.class);
            MinecraftForge.EVENT_BUS.register(VrGunConfig.class);
            MinecraftForge.EVENT_BUS.register(VrRecoil.class);
            MinecraftForge.EVENT_BUS.register(VrOffhandShooting.class);
            MinecraftForge.EVENT_BUS.register(VrDualReload.class);
            MinecraftForge.EVENT_BUS.register(VrGrabReload.class);
            MinecraftForge.EVENT_BUS.register(VrPrecision.class);
            MinecraftForge.EVENT_BUS.register(VrGripHint.class);
            MinecraftForge.EVENT_BUS.register(VrBowLaser.class);
            MinecraftForge.EVENT_BUS.register(VrThrow.class);
        }
        VR.preload();
        LOGGER.info("VR backend: {}, VR active at startup: {}", VR.description(), VR.active());
    }

    /**
     * With {@code -Djegvr.audit=true} ({@code runClientAudit}): applies every mixin of every mod now, instead of as each
     * class is first used, then closes the game. A mixin that does not apply, ours or another mod's against ours, is in
     * the log. That is how a new version of JEG, Vivecraft or a renderer mod gets checked without playing.
     */
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        if (Boolean.getBoolean("jegvr.audit")) {
            event.enqueueWork(() -> {
                LOGGER.info("Mixin audit: applying everything");
                MixinEnvironment.getCurrentEnvironment().audit();
                LOGGER.info("Mixin audit finished");
                System.exit(0);
            });
        }
    }
}
