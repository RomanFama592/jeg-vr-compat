package com.jegvr.server;

import com.jegvr.JegVrCompat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.jegvr.config.VrRules;
import com.jegvr.config.VrServerConfig;
import com.jegvr.network.S2CVrRules;
import com.jegvr.network.VrNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S.8: who said hello ({@link com.jegvr.network.C2SVrHello}), with which protocol and whether in VR; sends them the
 * server's rules and sends them again when the file changes. No client classes, no Vivecraft (rule 1).
 */
public final class VrServerRules {
    private record Client(int protocol, boolean vr) {
    }

    private static final Logger LOGGER = LogManager.getLogger(JegVrCompat.MOD_ID);
    private static final Map<UUID, Client> CLIENTS = new ConcurrentHashMap<>();

    private VrServerRules() {
    }

    public static VrRules.Rules rules() {
        return VrServerConfig.rules();
    }

    public static void hello(ServerPlayer player, int protocol, boolean vr) {
        Client before = CLIENTS.put(player.getUUID(), new Client(protocol, vr));
        // VR turned on or off: the rules are the same, no need to send them again.
        if (before == null || before.protocol != protocol) {
            send(player, protocol);
        }
    }

    /**
     * True for a player whose game says it plays in VR. Checks that depend on it only make a VR player follow what a
     * flat player already does in JEG, so a client lying about it gains nothing.
     */
    public static boolean isVr(Player player) {
        Client client = CLIENTS.get(player.getUUID());
        return client != null && client.vr;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CLIENTS.remove(event.getEntity().getUUID());
    }

    /** Mod bus: the server's file was edited while the server runs. */
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != VrServerConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Client client = CLIENTS.get(player.getUUID());
                if (client != null) {
                    send(player, client.protocol);
                }
            }
        });
        LOGGER.info("VR server rules reloaded: {}", VrServerConfig.rules());
    }

    private static void send(ServerPlayer player, int protocol) {
        if (protocol >= VrNetwork.RULES_PROTOCOL) {
            VrNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CVrRules(rules()));
        }
    }
}
