package com.jegvr.client;

import com.jegvr.compat.jeg.JegKeys;
import com.jegvr.compat.VR;
import com.jegvr.mixin.input.ReloadHandlerAccessor;
import com.jegvr.network.C2SVrAttachmentsOffhand;
import com.jegvr.network.C2SVrReloadOffhand;
import com.jegvr.network.C2SVrUnloadOffhand;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrOffhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import ttv.migami.jeg.animations.GunAnimations;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.client.screen.AttachmentScreen;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSyncedDataKeys;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Which gun JEG's keys act on in VR. Reload (and in VR also JEG's aim key: aiming down sights does not work in VR, see
 * {@code AimingHandlerVRMixin}), unload, inspect and attachments act on the gun whose barrel points up, the main-hand
 * gun first when both do. If neither points up it is the main-hand gun; with a gun only in the off hand, that one.
 */
public final class VrDualReload {
    /** Barrel at least ~30° above the horizon. */
    private static final double UP = 0.5;

    /** How long the attachments screen of the off-hand gun may take to arrive from the server. */
    private static final int ATTACHMENTS_MENU_WAIT_TICKS = 40;

    private static boolean aimWasDown;
    private static int attachmentsMenuWait;

    private VrDualReload() {
    }

    /** Called before JEG handles a key event; true when the key acted on the off-hand gun and JEG must skip the event. */
    public static boolean onKeyPressed(int action) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !actsOnOffhand(player)) {
            return false;
        }
        if (action == GLFW.GLFW_PRESS && KeyBinds.KEY_RELOAD.isDown()) {
            reloadOffhand(player);
            return true;
        }
        // JEG takes this key's click whichever the event is, and only acts when it is a press.
        if (KeyBinds.KEY_UNLOAD.consumeClick() && action == GLFW.GLFW_PRESS) {
            ItemStack stack = player.getOffhandItem();
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.putBoolean(JegKeys.IS_RELOADING, false);
            }
            ReloadHandler.get().setReloading(false);
            VrNetwork.CHANNEL.sendToServer(new C2SVrUnloadOffhand());
            // JEG's unload clears the gun's draw counter, so a main-hand gun plays its draw animation again.
            if (stack.getItem() instanceof AnimatedGunItem gun) {
                VrHands.drawOffhand(GunEnchantmentHelper.getModifiedDrawTick(stack,
                        gun.getModifiedGun(stack).getGeneral().getDrawTimer()));
            }
            return true;
        }
        return false;
    }

    /** JEG's inspect key. The animation is the client's: JEG's server only sets a tag that says to play it. */
    public static boolean onInspectKey(int action) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || action != GLFW.GLFW_PRESS || !KeyBinds.KEY_INSPECT.isDown() || !actsOnOffhand(player)) {
            return false;
        }
        ItemStack stack = player.getOffhandItem();
        if (stack.getItem() instanceof AnimatedGunItem gun) {
            AnimationController<GeoAnimatable> controller = gun.getAnimatableInstanceCache()
                    .<GeoAnimatable>getManagerForId(GeoItem.getId(stack)).getAnimationControllers().get("Controller");
            if (controller != null && controller.getCurrentAnimation() != null
                    && GunAnimations.isAnimationPlaying(controller, "inspect")) {
                controller.forceAnimationReset();
            }
            VrHands.inspectOffhand();
        }
        return true;
    }

    /** JEG's attachments key: asks the server for the screen of the off-hand gun. */
    public static boolean onAttachmentsKey(int action) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || action != GLFW.GLFW_PRESS || !KeyBinds.KEY_ATTACHMENTS.isDown()
                || !actsOnOffhand(player)) {
            return false;
        }
        if (!player.getOffhandItem().is(ModItems.FINGER_GUN.get())) {
            attachmentsMenuWait = ATTACHMENTS_MENU_WAIT_TICKS;
            VrOffhand.setClientMenuForOffhand(true);
            VrNetwork.CHANNEL.sendToServer(new C2SVrAttachmentsOffhand());
        }
        return true;
    }

    /** The screen reads the off-hand gun until it closes, or if the server never opens it. */
    private static void tickAttachmentsMenu(Minecraft mc) {
        if (!VrOffhand.clientMenuForOffhand()) {
            return;
        }
        if (mc.screen instanceof AttachmentScreen) {
            attachmentsMenuWait = 0;
        } else if (attachmentsMenuWait > 0) {
            attachmentsMenuWait--;
        } else {
            VrOffhand.setClientMenuForOffhand(false);
        }
    }

    /** The aim key, pressed in VR with a gun in hand, reloads instead. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean down = player != null && mc.screen == null && KeyBinds.getAimMapping().isDown() && VR.active();
        if (down && !aimWasDown) {
            if (actsOnOffhand(player)) {
                reloadOffhand(player);
            } else if (player.getMainHandItem().getItem() instanceof GunItem) {
                reloadMainHand(player);
            }
        }
        aimWasDown = down;
        if (player != null) {
            VrHands.tick(ModSyncedDataKeys.RELOADING.getValue(player));
        }
        tickAttachmentsMenu(mc);
    }

    private static boolean actsOnOffhand(LocalPlayer player) {
        if (!VrOffhand.canDualWield(player.getOffhandItem()) || !VR.active() || !VrAim.serverHasAddon()) {
            return false;
        }
        boolean mainGun = player.getMainHandItem().getItem() instanceof GunItem;
        return !mainGun || (pointsUp(VR.offHandAim()) && !pointsUp(VR.mainHandAim()));
    }

    private static boolean pointsUp(VR.Aim aim) {
        return aim != null && aim.dir().lengthSqr() > 1.0E-6 && aim.dir().normalize().y >= UP;
    }

    /** What JEG's reload key does (ReloadHandler.onKeyPressed). */
    public static void reloadMainHand(LocalPlayer player) {
        CompoundTag tag = player.getMainHandItem().getTag();
        if (tag != null) {
            tag.putBoolean(JegKeys.IS_RELOADING, true);
        }
        ReloadHandler.get().setReloading(true);
    }

    /** Reloads the off-hand gun (also JEG's automatic reload when its trigger is pulled empty). */
    public static void reloadOffhand(LocalPlayer player) {
        ItemStack stack = player.getOffhandItem();
        GunItem gunItem = (GunItem) stack.getItem();
        Gun gun = gunItem.getModifiedGun(stack);
        CompoundTag tag = stack.getTag();
        if (ModSyncedDataKeys.RELOADING.getValue(player) || tag == null || tag.getBoolean(JegKeys.IGNORE_AMMO)
                || tag.getInt(JegKeys.AMMO_COUNT) >= GunModifierHelper.getModifiedAmmoCapacity(stack, gun)) {
            return;
        }
        // Mirror JEG's ReloadHandler.setReloading(true): predict the flag, start the reload animation, and record the
        // slot so JEG's slot check does not cancel it.
        tag.putBoolean(JegKeys.IS_RELOADING, true);
        ModSyncedDataKeys.RELOADING.setValue(player, true);
        VrHands.markReload(InteractionHand.OFF_HAND);
        ((ReloadHandlerAccessor) ReloadHandler.get()).jegvr$setReloadingSlot(player.getInventory().selected);
        VrNetwork.CHANNEL.sendToServer(new C2SVrReloadOffhand());
    }
}
