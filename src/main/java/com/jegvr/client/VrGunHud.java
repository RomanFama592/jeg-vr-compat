package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.init.ModEnchantments;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.modifier.Modifier;
import ttv.migami.jeg.modifier.ModifierHelper;

/**
 * F9.9: each gun's name, loaded rounds and reserve float beside the hand that holds it. It shows what JEG's ammo overlay
 * ({@code GuiRenderer}) shows for the main-hand gun, which this replaces while it is on (see {@code GuiRendererVRMixin}).
 * <p>
 * The panel follows the controller, on the side towards the body so it does not cover the sights, and always faces the
 * headset, so both eyes see the same flat panel.
 */
public final class VrGunHud {
    /** Where the panel's centre sits, in room-scale metres along the controller's axes. */
    private static final double INNER = 0.11;
    private static final double BACK = 0.05;
    private static final double UP = 0.03;
    /** Room-scale metres per font pixel: the name line is about 1.4 cm tall. */
    private static final float TEXT_SCALE = 0.0015F;
    private static final float AMMO_SCALE = 2.0F;
    private static final int NAME_Y = -11;
    private static final int RESERVE_GAP = 4;
    /** JEG's colours for the rounds and the reserve. */
    private static final int AMMO_COLOR = 0xFFFFFF;
    private static final int RESERVE_COLOR = 0x878787;

    private VrGunHud() {
    }

    /** The panels are showing instead of JEG's overlay: VR with tracked hands and the option on. */
    public static boolean active() {
        return VR.active() && VrClientConfig.gunHud() && !VR.seated();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !active() || !VR.isEyePass()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || mc.options.hideGui || !Config.CLIENT.display.showAmmoGUI.get()) {
            return;
        }
        VR.Aim hmd = VR.hmdAim();
        Vec3 hmdUp = VR.hmdUp();
        if (hmd == null || hmdUp == null) {
            return;
        }
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (shows(stack)) {
                Vec3 anchor = anchor(hand);
                if (anchor != null) {
                    pose.pushPose();
                    pose.translate(anchor.x - cam.x, anchor.y - cam.y, anchor.z - cam.z);
                    pose.mulPose(facing(hmd.pos().subtract(anchor), hmdUp));
                    float scale = TEXT_SCALE * VR.worldScale();
                    // Font pixels: x right, y down, towards the viewer in z so the text sits over its shadow.
                    pose.scale(scale, -scale, scale);
                    draw(mc.font, player, stack, hand, pose, buffers);
                    pose.popPose();
                }
            }
        }
        buffers.endBatch();
    }

    /** The guns JEG's overlay shows: not the Finger Gun, and only once the gun has its ammo tag. */
    private static boolean shows(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return stack.getItem() instanceof GunItem && !stack.is(ModItems.FINGER_GUN.get()) && tag != null
                && tag.contains("AmmoCount");
    }

    private static Vec3 anchor(InteractionHand hand) {
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        VR.Aim aim = mainHand ? VR.mainHandAim() : VR.offHandAim();
        Vec3[] axes = VR.handAxes(mainHand);
        if (aim == null || axes == null) {
            return null;
        }
        // The body side of a left controller is its +X, of a right one its -X.
        double inner = mainHand == VR.leftHanded() ? INNER : -INNER;
        double worldScale = VR.worldScale();
        return aim.pos().add(axes[0].scale(inner * worldScale))
                .add(axes[1].scale(UP * worldScale))
                .add(axes[2].scale(BACK * worldScale));
    }

    /** Maps X to the panel's right, Y to its up and Z towards the headset; "up" follows the headset's. */
    private static Quaternionf facing(Vec3 toHmd, Vec3 hmdUp) {
        Vec3 z = toHmd.normalize();
        Vec3 x = hmdUp.cross(z).normalize();
        Vec3 y = z.cross(x);
        return new Quaternionf().setFromNormalized(new Matrix3f(
                (float) x.x, (float) x.y, (float) x.z,
                (float) y.x, (float) y.y, (float) y.z,
                (float) z.x, (float) z.y, (float) z.z));
    }

    private static void draw(Font font, LocalPlayer player, ItemStack stack, InteractionHand hand, PoseStack pose,
                             MultiBufferSource buffers) {
        Gun gun = ((GunItem) stack.getItem()).getModifiedGun(stack);
        String name = stack.getHoverName().getString();
        text(font, name, -font.width(name) / 2.0F, NAME_Y, nameColor(stack), pose, buffers);

        boolean reloading = ModSyncedDataKeys.RELOADING.getValue(player) && VrHands.reloadingHand() == hand;
        String ammo = reloading ? Component.translatable("jeg_vr_compat.hud.reloading").getString()
                : String.format("%03d", stack.getTag().getInt("AmmoCount"));
        String reserve = reloading ? "" : reserve(player, stack, gun);
        float ammoWidth = font.width(ammo) * AMMO_SCALE;
        float left = -(ammoWidth + (reserve.isEmpty() ? 0 : RESERVE_GAP + font.width(reserve))) / 2.0F;
        pose.pushPose();
        pose.translate(left, 0.0F, 0.0F);
        pose.scale(AMMO_SCALE, AMMO_SCALE, 1.0F);
        text(font, ammo, 0.0F, 0.0F, AMMO_COLOR, pose, buffers);
        pose.popPose();
        if (!reserve.isEmpty()) {
            text(font, reserve, left + ammoWidth + RESERVE_GAP, 0.0F, RESERVE_COLOR, pose, buffers);
        }
    }

    private static void text(Font font, String text, float x, float y, int color, PoseStack pose, MultiBufferSource buffers) {
        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, x, y, color, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    /** As JEG's overlay: the rarity's colour, or the colour of the gun's modifier. */
    private static int nameColor(ItemStack stack) {
        String modifier = stack.getOrCreateTag().getString("CustomModifier");
        Modifier group = modifier.isEmpty() ? null : ModifierHelper.getGroupByName(modifier);
        if (group != null) {
            return group.getColor();
        }
        Integer color = stack.getRarity().color.getColor();
        return color == null ? AMMO_COLOR : color;
    }

    /** As JEG's overlay: the rounds left in the inventory for this gun. */
    private static String reserve(LocalPlayer player, ItemStack stack, Gun gun) {
        if (player.isCreative() || stack.getEnchantmentLevel(ModEnchantments.INFINITY.get()) != 0
                || stack.getTag().getBoolean("IgnoreAmmo")) {
            return "9999";
        }
        if (gun.getReloads().getReloadType().equals(ReloadType.SINGLE_ITEM)) {
            int items = Gun.findAmmoStack(player, gun.getReloads().getReloadItem()).length;
            return String.format("%04d", items * gun.getReloads().getMaxAmmo());
        }
        return String.format("%04d", Gun.getTotalAmmoCount(Gun.findAmmoStack(player, gun.getProjectile().getItem())));
    }
}
