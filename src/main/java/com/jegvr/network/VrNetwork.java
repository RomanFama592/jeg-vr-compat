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
    /** Bump when a packet changes; a mismatch (not an absence) is rejected on join. */
    private static final String VERSION = "6";

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
        CHANNEL.messageBuilder(C2SVrReloadOffhand.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrReloadOffhand::encode)
                .decoder(C2SVrReloadOffhand::decode)
                .consumerMainThread(C2SVrReloadOffhand::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrInsertRound.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrInsertRound::encode)
                .decoder(C2SVrInsertRound::decode)
                .consumerMainThread(C2SVrInsertRound::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrPreFireOffhand.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrPreFireOffhand::encode)
                .decoder(C2SVrPreFireOffhand::decode)
                .consumerMainThread(C2SVrPreFireOffhand::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrMeleeOffhand.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrMeleeOffhand::encode)
                .decoder(C2SVrMeleeOffhand::decode)
                .consumerMainThread(C2SVrMeleeOffhand::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrUnloadOffhand.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrUnloadOffhand::encode)
                .decoder(C2SVrUnloadOffhand::decode)
                .consumerMainThread(C2SVrUnloadOffhand::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrAttachmentsOffhand.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrAttachmentsOffhand::encode)
                .decoder(C2SVrAttachmentsOffhand::decode)
                .consumerMainThread(C2SVrAttachmentsOffhand::handle)
                .add();
    }

    public static boolean isPresent(Connection connection) {
        return connection != null && CHANNEL.isRemotePresent(connection);
    }
}
