package com.jegvr.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.item.GunItem;

/** Client commands to calibrate gun poses with the headset on. */
public final class VrDebugCommands {
    private VrDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("jegvr")
                .then(Commands.literal("pose")
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg())
                                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                                        .then(Commands.argument("roll", FloatArgumentType.floatArg())
                                                .executes(c -> setPose(c, false))
                                                .then(Commands.argument("tx", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("ty", FloatArgumentType.floatArg())
                                                                .then(Commands.argument("tz", FloatArgumentType.floatArg())
                                                                        .executes(c -> setPose(c, true)))))))))
                .then(Commands.literal("save").executes(c -> {
                    VrGunConfig.save();
                    return reply(c, "Guardado en config/jeg_vr_compat/guns.json");
                }))
                .then(Commands.literal("reset").executes(c -> {
                    String id = heldGunId();
                    VrGunConfig.Pose pose = VrGunConfig.editable(id);
                    pose.set(new VrGunConfig.Pose());
                    return reply(c, "Pose restablecida para " + (id == null ? "default" : id));
                })));
    }

    private static int setPose(CommandContext<CommandSourceStack> c, boolean withPosition) {
        String id = heldGunId();
        VrGunConfig.Pose pose = VrGunConfig.editable(id);
        pose.pitch = FloatArgumentType.getFloat(c, "pitch");
        pose.yaw = FloatArgumentType.getFloat(c, "yaw");
        pose.roll = FloatArgumentType.getFloat(c, "roll");
        if (withPosition) {
            pose.tx = FloatArgumentType.getFloat(c, "tx");
            pose.ty = FloatArgumentType.getFloat(c, "ty");
            pose.tz = FloatArgumentType.getFloat(c, "tz");
        }
        return reply(c, String.format("%s: pitch=%.1f yaw=%.1f roll=%.1f t=(%.3f, %.3f, %.3f). /jegvr save para guardar",
                id == null ? "default (sin arma en mano)" : id, pose.pitch, pose.yaw, pose.roll, pose.tx, pose.ty, pose.tz));
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
