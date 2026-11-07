package com.jegvr.compat;

import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Everything the addon needs from a VR mod. One implementation per supported line of Vivecraft, chosen by {@link VR} at
 * startup; nothing else outside the mixins touches {@code org.vivecraft.*}. A method may throw: {@link VR} catches it and
 * turns VR support off for the session.
 */
public interface VrBackend {
    /** The name logged at startup, for bug reports. */
    String name();

    boolean isActive();

    boolean isVRPlayer(Player player);

    /** World-space pose of the controller of the main hand (it follows Vivecraft's left-handed setting), or null. */
    VR.Aim mainHand();

    VR.Aim offHand();

    VR.Aim hmd();

    Vec3 hmdUp();

    /** {@code dir} (world space) in a controller's own axes, the frame the gun held in that hand is drawn in. */
    Vec3 toHandLocal(boolean mainHand, Vec3 dir);

    /** A controller's rotation and position in the room, in metres: no world position, rotation or scale. */
    Matrix4f roomHand(boolean mainHand);

    /** A controller's own X, Y and Z axes in world space. */
    Vec3[] handAxes(boolean mainHand);

    /** True while the left eye is drawn, the first view of every frame. */
    boolean isLeftEyePass();

    /** False while a view that is not the player's own eyes is drawn (third-person or mixed-reality camera). */
    boolean isEyePass();

    /** Vibrates a controller; {@code mainHand} false targets the free hand. */
    void haptic(boolean mainHand, float seconds, float amplitude);

    /** The teleport keys (teleport mode and free-move fallback), bound to the off-hand trigger by default. */
    KeyMapping[] teleportKeys();

    /** The key on the off-hand grip with the default bindings. */
    KeyMapping offHandGripKey();

    /** The key on the main-hand grip with the default bindings. */
    KeyMapping mainHandGripKey();

    /** Whether {@code key} is bound to the main-hand controller (true), the off-hand one (false), or neither or both (null). */
    Boolean boundToMainHand(KeyMapping key);

    /** Roomscale bow: drawn with the off-hand trigger, so that trigger must keep its normal role. */
    boolean isBow(ItemStack stack);

    /** True when the HUD (with the hotbar) is locked to the off-hand controller, not to the head. */
    boolean isHudOnHand();

    /** Seated mode: the hands follow the view, they are not tracked. */
    boolean isSeated();

    boolean isLeftHanded();

    float worldScale();

    /** True with a shader pack on (Iris, Oculus or OptiFine), as the VR mod sees it. */
    boolean isShaderActive();
}
