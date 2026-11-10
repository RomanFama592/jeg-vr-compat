package com.jegvr.client;

import com.jegvr.config.VrClientConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

/**
 * Client commands to calibrate gun poses with the headset on. Values are always the final pose of the held gun
 * (global default included); without a gun in hand they edit the global default. The texts are in the lang files.
 */
public final class VrDebugCommands {
    private static final String KEY = "jeg_vr_compat.command.";
    private static final int CAPTURE_SECONDS = 5;
    private static final int HELP_LINES = 8;

    private VrDebugCommands() {
    }

    private static FloatArgumentType f() {
        return FloatArgumentType.floatArg();
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("jegvr");

        // /jegvr pose <pitch> <yaw> <roll> [tx ty tz]  -> absolute
        root.then(Commands.literal("pose")
                .then(Commands.argument("pitch", f()).then(Commands.argument("yaw", f()).then(Commands.argument("roll", f())
                        .executes(c -> edit(c, p -> {
                            p.pitch = FloatArgumentType.getFloat(c, "pitch");
                            p.yaw = FloatArgumentType.getFloat(c, "yaw");
                            p.roll = FloatArgumentType.getFloat(c, "roll");
                        }))
                        .then(Commands.argument("tx", f()).then(Commands.argument("ty", f()).then(Commands.argument("tz", f())
                                .executes(c -> edit(c, p -> {
                                    p.pitch = FloatArgumentType.getFloat(c, "pitch");
                                    p.yaw = FloatArgumentType.getFloat(c, "yaw");
                                    p.roll = FloatArgumentType.getFloat(c, "roll");
                                    p.tx = FloatArgumentType.getFloat(c, "tx");
                                    p.ty = FloatArgumentType.getFloat(c, "ty");
                                    p.tz = FloatArgumentType.getFloat(c, "tz");
                                })))))))));

        // /jegvr rot <dpitch> <dyaw> <droll>  and  /jegvr move <dx> <dy> <dz>  -> relative nudges
        root.then(Commands.literal("rot")
                .then(Commands.argument("dpitch", f()).then(Commands.argument("dyaw", f()).then(Commands.argument("droll", f())
                        .executes(c -> edit(c, p -> {
                            p.pitch += FloatArgumentType.getFloat(c, "dpitch");
                            p.yaw += FloatArgumentType.getFloat(c, "dyaw");
                            p.roll += FloatArgumentType.getFloat(c, "droll");
                        }))))));
        root.then(Commands.literal("move")
                .then(Commands.argument("dx", f()).then(Commands.argument("dy", f()).then(Commands.argument("dz", f())
                        .executes(c -> edit(c, p -> {
                            p.tx += FloatArgumentType.getFloat(c, "dx");
                            p.ty += FloatArgumentType.getFloat(c, "dy");
                            p.tz += FloatArgumentType.getFloat(c, "dz");
                        }))))));
        root.then(Commands.literal("scale")
                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F, 10.0F))
                        .executes(c -> edit(c, p -> p.scale = FloatArgumentType.getFloat(c, "scale")))));

        root.then(Commands.literal("show").executes(c -> edit(c, p -> {
        })));
        root.then(Commands.literal("zones").executes(c ->
                reply(c, Component.translatable(KEY + (VrGrabReload.toggleZones() ? "zones.on" : "zones.off")))));
        LiteralArgumentBuilder<CommandSourceStack> grabZone = Commands.literal("grabzone");
        for (VrClientConfig.GrabZone zone : VrClientConfig.GrabZone.values()) {
            String name = zone.name().toLowerCase(Locale.ROOT);
            grabZone.then(Commands.literal(name).executes(c -> {
                VrClientConfig.setGrabZone(zone);
                return reply(c, Component.translatable(KEY + "grabzone." + name));
            }));
        }
        root.then(grabZone);
        // F10.4: the two-handed grip of the held gun.
        root.then(Commands.literal("grip")
                .then(Commands.literal("capture")
                        .executes(c -> startCapture(c, CAPTURE_SECONDS))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 30))
                                .executes(c -> startCapture(c, IntegerArgumentType.getInteger(c, "seconds")))))
                .then(Commands.literal("reset").executes(c -> reply(c, VrGripHint.resetGrip())))
                .then(Commands.literal("ghost").executes(c -> {
                    VrGripHint.showGhost(0);
                    return reply(c, Component.translatable(KEY + "grip.ghost"));
                })));
        root.then(Commands.literal("help").executes(c -> {
            for (int line = 0; line < HELP_LINES; line++) {
                reply(c, Component.translatable(KEY + "help." + line));
            }
            return 1;
        }));
        root.then(Commands.literal("save").executes(c -> {
            java.nio.file.Path path = VrGunConfig.save();
            if (path == null) {
                return reply(c, Component.translatable(KEY + "save.failed"));
            }
            return reply(c, Component.translatable(KEY + (VrGunConfig.editsBuiltIn() ? "save.builtin" : "save.user"), path.toString()));
        }));
        root.then(Commands.literal("reset").executes(c -> {
            String id = heldGunId();
            VrGunConfig.reset(id);
            Component what = poseName(id);
            if (!VrGunConfig.editsBuiltIn()) {
                return reply(c, Component.translatable(KEY + "reset.user", what));
            }
            return reply(c, Component.translatable(KEY + (id == null ? "reset.builtin.defaults" : "reset.builtin.gun"), what));
        }));
        d.register(root);
    }

    private static int startCapture(CommandContext<CommandSourceStack> c, int seconds) {
        VrGripHint.startCapture(seconds);
        return reply(c, Component.translatable(KEY + "grip.countdown", seconds));
    }

    private static Component poseName(String gunId) {
        return gunId == null ? Component.translatable(KEY + "pose.defaults") : Component.literal(gunId);
    }

    private static int edit(CommandContext<CommandSourceStack> c, java.util.function.Consumer<VrGunConfig.Pose> change) {
        String id = heldGunId();
        VrGunConfig.Pose pose = VrGunConfig.poseFor(id == null ? "" : id);
        change.accept(pose);
        VrGunConfig.setEffective(id, pose);
        return reply(c, Component.empty().append(poseName(id)).append(String.format(
                ": pitch=%.1f yaw=%.1f roll=%.1f | t=(%.3f, %.3f, %.3f) | scale=%.2f  (/jegvr save)",
                pose.pitch, pose.yaw, pose.roll, pose.tx, pose.ty, pose.tz, pose.scale)));
    }

    /** Registry id of the gun in the main hand, or null (edits the global default then). */
    private static String heldGunId() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return VrGunPose.heldLikeGun(stack) ? VrGunPose.gunId(stack) : null;
    }

    private static int reply(CommandContext<CommandSourceStack> c, Component message) {
        c.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}
