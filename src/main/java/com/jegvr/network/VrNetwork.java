package com.jegvr.network;

import com.jegvr.JegVrCompat;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Optional channel: either side may lack it (a JEG-only server, or a server with the addon and a flat client without
 * it). The client only sends on it after checking {@link #isPresent(Connection)}.
 */
public final class VrNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JegVrCompat.MOD_ID, "main"), () -> VERSION,
            NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    private VrNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(C2SVrAim.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrAim::encode)
                .decoder(C2SVrAim::decode)
                .consumerMainThread(C2SVrAim::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrShootOffhand.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrShootOffhand::encode)
                .decoder(C2SVrShootOffhand::decode)
                .consumerMainThread(C2SVrShootOffhand::handle)
                .add();
    }

    public static boolean isPresent(Connection connection) {
        return connection != null && CHANNEL.isRemotePresent(connection);
    }
}
