package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrRules;
import com.jegvr.network.C2SVrHello;
import com.jegvr.network.VrNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.ref.WeakReference;

/**
 * S.8: the rules of the server this client plays on. Says hello to a server with the addon (protocol and VR on or off),
 * keeps what it answers, and forgets it on leaving. Without an answer every option is the player's
 * ({@link VrRules.Rules#PLAYER_CHOICE}).
 */
public final class VrRulesClient {
    private static volatile VrRules.Rules rules = VrRules.Rules.PLAYER_CHOICE;
    /** F9.8: apart from {@link #rules}, in its own packet. */
    private static volatile VrRules.Toggle selfDamageRule = VrRules.Toggle.PLAYER;
    private static WeakReference<Connection> helloSentTo = new WeakReference<>(null);
    private static boolean helloVr;
    private static boolean noticeShown;

    private VrRulesClient() {
    }

    public static VrRules.Rules rules() {
        return rules;
    }

    public static VrRules.Toggle selfDamageRule() {
        return selfDamageRule;
    }

    /** Network thread: the server's answer. */
    public static void receive(VrRules.Rules received) {
        rules = received;
        noticeIfDecided(received.decidesAny());
    }

    /** Network thread: the server's self-damage rule (F9.8). */
    public static void receiveSelfDamage(VrRules.Toggle rule) {
        selfDamageRule = rule;
        noticeIfDecided(rule != VrRules.Toggle.PLAYER);
    }

    /** Once per server: tells a VR player that the server decides some options. */
    private static void noticeIfDecided(boolean decides) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!noticeShown && decides && mc.player != null && VR.active()) {
                noticeShown = true;
                mc.player.displayClientMessage(Component.translatable("jeg_vr_compat.rules.notice"), false);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        Connection connection = listener == null ? null : listener.getConnection();
        if (connection == null || VrNetwork.serverProtocol(connection) < VrNetwork.RULES_PROTOCOL) {
            return;
        }
        boolean vr = VR.active();
        if (helloSentTo.get() != connection || helloVr != vr) {
            helloSentTo = new WeakReference<>(connection);
            helloVr = vr;
            VrNetwork.CHANNEL.sendToServer(new C2SVrHello(VrNetwork.PROTOCOL, vr));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        rules = VrRules.Rules.PLAYER_CHOICE;
        selfDamageRule = VrRules.Toggle.PLAYER;
        helloSentTo = new WeakReference<>(null);
        noticeShown = false;
    }
}
