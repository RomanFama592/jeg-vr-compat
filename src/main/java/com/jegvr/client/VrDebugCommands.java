package com.jegvr.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.item.GunItem;

/**
 * Client commands to calibrate gun poses with the headset on. Values are always the final pose of the held gun
 * (global default included); without a gun in hand they edit the global default.
 */
public final class VrDebugCommands {
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
                                }))))))));

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
        root.then(Commands.literal("save").executes(c -> {
            VrGunConfig.save();
            return reply(c, "Guardado en config/jeg_vr_compat/guns.json");
        }));
        root.then(Commands.literal("reset").executes(c -> {
            String id = heldGunId();
            VrGunConfig.setEffective(id, VrGunConfig.factoryDefaults());
            return reply(c, "Pose restablecida para " + (id == null ? "default" : id));
        }));
        d.register(root);
    }

    private static int edit(CommandContext<CommandSourceStack> c, java.util.function.Consumer<VrGunConfig.Pose> change) {
        String id = heldGunId();
        VrGunConfig.Pose pose = VrGunConfig.poseFor(id == null ? "" : id);
        change.accept(pose);
        VrGunConfig.setEffective(id, pose);
        return reply(c, String.format("%s: pitch=%.1f yaw=%.1f roll=%.1f | t=(%.3f, %.3f, %.3f) | scale=%.2f  (/jegvr save)",
                id == null ? "default (sin arma en mano)" : id, pose.pitch, pose.yaw, pose.roll,
                pose.tx, pose.ty, pose.tz, pose.scale));
    }

    /** Registry id of the gun in the main hand, or null (edits the global default then). */
    private static String heldGunId() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof GunItem ? VrGunPose.gunId(stack) : null;
    }

    private static int reply(CommandContext<CommandSourceStack> c, String message) {
        c.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
