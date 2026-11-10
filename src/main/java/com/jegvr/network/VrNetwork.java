package com.jegvr.network;

import com.jegvr.JegVrCompat;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ConnectionData;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.ref.WeakReference;

/**
 * Optional channel: either side may lack it (a JEG-only server, or a server with the addon and a flat client without
 * it), and the two sides may run different releases of the addon. Nothing is rejected on join: the client reads the
 * server's {@link #PROTOCOL} and only sends what that server understands.
 * <p>
 * The contract between releases: a packet id only ever gets added, and an existing packet never changes its format. A new
 * packet raises {@link #PROTOCOL}, and the client checks {@link #serverProtocol} before sending it.
 */
public final class VrNetwork {
    /** The packets below are all there since this one, the first public protocol. */
    private static final int BASE_PROTOCOL = 6;
    /** Adds {@link C2SVrThrow} (F9.26). */
    public static final int THROW_PROTOCOL = 7;
    /** Adds {@link C2SVrHello} and {@link S2CVrRules} (S.8, server rules). */
    public static final int RULES_PROTOCOL = 8;
    /** Adds {@link C2SVrSelfTargets} and {@link S2CVrSelfDamageRule} (F9.8). */
    public static final int SELF_DAMAGE_PROTOCOL = 9;
    public static final int PROTOCOL = SELF_DAMAGE_PROTOCOL;

    private static final ResourceLocation NAME = new ResourceLocation(JegVrCompat.MOD_ID, "main");

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(NAME, () -> Integer.toString(PROTOCOL),
            remote -> true, remote -> true);

    private static WeakReference<Connection> readConnection = new WeakReference<>(null);
    private static int readProtocol;

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
        CHANNEL.messageBuilder(C2SVrThrow.class, 8, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrThrow::encode)
                .decoder(C2SVrThrow::decode)
                .consumerMainThread(C2SVrThrow::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrHello.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrHello::encode)
                .decoder(C2SVrHello::decode)
                .consumerMainThread(C2SVrHello::handle)
                .add();
        CHANNEL.messageBuilder(S2CVrRules.class, 10, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CVrRules::encode)
                .decoder(S2CVrRules::decode)
                .consumerNetworkThread(S2CVrRules::handle)
                .add();
        CHANNEL.messageBuilder(C2SVrSelfTargets.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SVrSelfTargets::encode)
                .decoder(C2SVrSelfTargets::decode)
                .consumerMainThread(C2SVrSelfTargets::handle)
                .add();
        CHANNEL.messageBuilder(S2CVrSelfDamageRule.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CVrSelfDamageRule::encode)
                .decoder(S2CVrSelfDamageRule::decode)
                .consumerNetworkThread(S2CVrSelfDamageRule::handle)
                .add();
    }

    /** True when the other side has this channel with at least the first public protocol. */
    public static boolean isPresent(Connection connection) {
        return serverProtocol(connection) >= BASE_PROTOCOL;
    }

    /** The protocol of the addon on the other side of {@code connection}, or 0 without it. Read once per connection. */
    public static int serverProtocol(Connection connection) {
        if (connection == null) {
            return 0;
        }
        if (readConnection.get() != connection) {
            ConnectionData data = NetworkHooks.getConnectionData(connection);
            if (data == null) {
                // The handshake is not over yet: ask again next time.
                return 0;
            }
            readProtocol = parse(data.getChannels().get(NAME));
            readConnection = new WeakReference<>(connection);
        }
        return readProtocol;
    }

    private static int parse(String version) {
        try {
            return version == null ? 0 : Integer.parseInt(version);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
