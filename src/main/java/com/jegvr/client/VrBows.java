package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import ttv.migami.jeg.common.FireMode;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.item.AnimatedBowItem;

/**
 * F9.7: JEG's bows (compound and primitive) work like Vivecraft's roomscale bow. Vivecraft's {@code BowTracker} is told
 * they are bows ({@code BowTrackerVRMixin}), so it handles everything as for the vanilla bow: the bow goes to the
 * off-hand controller and an arrow to the main one, the arrow is notched by touching the string, the grip (Vivecraft's
 * "interact") draws it, and letting go shoots. Vivecraft's own release does nothing for a JEG gun; instead JEG's shoot
 * key reads as held while the string is drawn ({@code ShootingHandlerVRMixin}), so JEG charges and fires the bow on
 * release as usual, with the charge set to how far the string was pulled ({@code ChargeTrackerVRMixin}) and the arrow
 * flying from the bow along the line from the arrow hand ({@link VrAim}).
 * <p>
 * Only with the bow in the main hand and the off hand empty, Vivecraft's bow mode "on" and this addon's option on;
 * otherwise the bow is a normal JEG gun (fired with the trigger).
 */
public final class VrBows {
    /** A remote player's hands closer than this (room-scale metres) are taken as an arrow on the string. */
    private static final double REMOTE_NOTCH_GAP = 0.3;

    private VrBows() {
    }

    /** A JEG bow, fired by releasing (JEG's {@code RELEASE_FIRE}), with the option on. */
    public static boolean isBowItem(ItemStack stack) {
        return stack.getItem() instanceof AnimatedBowItem bow && VrClientConfig.vrBow()
                && bow.getModifiedGun(stack).getGeneral().getFireMode() == FireMode.RELEASE_FIRE;
    }

    /**
     * For Vivecraft's {@code BowTracker.isBow}: true for the local player's main-hand JEG bow with the off hand empty.
     * JEG only fires the main-hand gun, and a gun in the off hand keeps working as a gun.
     */
    public static boolean isRoomscaleBow(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && stack == player.getMainHandItem() && player.getOffhandItem().isEmpty()
                && VR.bowModeOn() && isBowItem(stack);
    }

    /** The local player is using a JEG bow through Vivecraft's roomscale bow right now. */
    public static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && VR.bowActive() && isRoomscaleBow(player.getMainHandItem());
    }

    /** The string is being pulled: JEG's shoot key reads as held. */
    public static boolean drawing() {
        return active() && VR.bowDrawing();
    }

    /** How far the string is pulled, 0 to 1: JEG's charge while drawing. */
    public static float drawPercent() {
        return VR.bowDrawPercent();
    }

    /** With an arrow on the string: from the bow, pointing from the arrow hand to the bow hand. Otherwise null. */
    public static VR.Aim aim() {
        return active() && VR.bowNotched() ? VR.bowAim() : null;
    }

    /**
     * How the bow sits in the controller being drawn, when that controller holds the roomscale bow; null otherwise.
     * The bow is held like Vivecraft's: its limbs (model +Y) run along the controller, its top where the controller
     * points, and its arrow (model -Z) points out of the fist. With an arrow on the string it points where the arrow
     * flies instead, with the limbs kept as close to the controller as that allows.
     */
    public static Quaternionf localOrientation(boolean mainController) {
        if (!active() || mainController != VR.bowInMainController()) {
            return null;
        }
        Vec3[] hand = VR.bowHandAxes();
        if (hand == null) {
            return null;
        }
        VR.Aim aim = aim();
        Vec3 forward = VR.toHandLocal(mainController, aim != null ? aim.dir() : hand[0]);
        Vec3 limbs = VR.toHandLocal(mainController, hand[1]);
        if (forward == null || limbs == null) {
            return null;
        }
        // The controller's own forward axis is -Z: the bow's top goes that way.
        if (limbs.z > 0.0) {
            limbs = limbs.scale(-1.0);
        }
        Vec3 f = forward.normalize();
        Vec3 up = limbs.subtract(f.scale(limbs.dot(f)));
        if (f.lengthSqr() < 1.0E-6 || up.lengthSqr() < 1.0E-6) {
            return null;
        }
        up = up.normalize();
        Vec3 back = f.scale(-1.0);
        Vec3 right = up.cross(back);
        Matrix3f basis = new Matrix3f(
                (float) right.x, (float) right.y, (float) right.z,
                (float) up.x, (float) up.y, (float) up.z,
                (float) back.x, (float) back.y, (float) back.z);
        return new Quaternionf().setFromNormalized(basis);
    }

    /**
     * The arrow Vivecraft shows in the arrow hand and needs before notching (it asks {@code Player.getProjectile},
     * which is empty for a JEG gun): the bow's projectile item if the player has one, the bow is loaded, or in
     * creative. Empty otherwise.
     */
    public static ItemStack ammo(LocalPlayer player, ItemStack bow) {
        if (!(bow.getItem() instanceof AnimatedBowItem bowItem)) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = bowItem.getModifiedGun(bow).getProjectile().getItem();
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        if (player.isCreative() || Gun.hasAmmo(bow)) {
            return new ItemStack(item);
        }
        ItemStack found = Gun.findAmmo(player, id).stack();
        return found.isEmpty() ? ItemStack.EMPTY : found;
    }

    /**
     * Third person (other players, Vivecraft's cameras): this player holds a JEG bow the roomscale way, so the bow is
     * drawn in the off hand and the arrow in the main hand. Remote players' Vivecraft bow mode is not known; any
     * standing VR player with a JEG bow and an empty off hand counts.
     */
    public static boolean shownAsRoomscale(Player player) {
        if (!isBowItem(player.getMainHandItem()) || !player.getOffhandItem().isEmpty() || !VR.isVRPlayer(player)) {
            return false;
        }
        return player == Minecraft.getInstance().player ? active() : !VR.remoteSeated(player);
    }

    /** Third person: the arrow in the main hand, or empty once it is on the string. */
    public static ItemStack shownArrow(Player player) {
        boolean notched;
        if (player == Minecraft.getInstance().player) {
            notched = VR.bowNotched();
        } else {
            double gap = VR.remoteHandGap(player);
            notched = gap >= 0.0 && gap < REMOTE_NOTCH_GAP;
        }
        if (notched || !(player.getMainHandItem().getItem() instanceof AnimatedBowItem bowItem)) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(bowItem.getModifiedGun(player.getMainHandItem()).getProjectile().getItem());
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
