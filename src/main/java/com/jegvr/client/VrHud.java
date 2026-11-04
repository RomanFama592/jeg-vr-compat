package com.jegvr.client;

import com.jegvr.compat.VR;
import com.jegvr.config.VrClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import ttv.migami.jeg.item.GunItem;

/**
 * F9.14: Vivecraft's hand-locked HUD (and hotbar) sits on the off-hand controller. With a gun in that hand it is moved
 * so the gun neither covers it nor cuts through it ({@link VrClientConfig.HudPlacement}):
 * <ul>
 * <li>ELBOW: slid back along the forearm, towards the elbow, facing as Vivecraft faces it.</li>
 * <li>FOREARM: turned so the hotbar lies along the inner side of the forearm, like a strip on the sleeve, read by
 * turning the wrist.</li>
 * </ul>
 * Without a gun, with the HUD locked to the head, or with a menu open, Vivecraft's position is kept. So it is with the
 * guns listed in the config (by default the minigun, whose body runs along the forearm): {@link VrClientConfig#hudExcluded}.
 */
public final class VrHud {
    /** ELBOW: room-scale metres (multiplied by Vivecraft's world scale) towards the elbow, and up off the sleeve. */
    private static final double ELBOW_BACK = 0.16;
    private static final double ELBOW_UP = 0.03;
    /** FOREARM: where the middle of the hotbar sits, in metres from the controller along its own axes. */
    private static final double FOREARM_BACK = 0.17;
    private static final double FOREARM_INNER = 0.055;
    private static final double FOREARM_DOWN = 0.02;
    /** Vivecraft's hotbar row, in HUD units below the HUD's anchor (MCVR.processHotbar uses -0.38 * scale). */
    private static final float HOTBAR_ROW = -0.38F;

    private VrHud() {
    }

    /**
     * Where the HUD goes this frame. May rewrite {@code rotation} and {@code offset} (Vivecraft's own objects, used
     * right after for drawing and for touching the hotbar); returns the new anchor position.
     */
    public static Vec3 place(Vec3 guiPos, Matrix4f rotation, Vector3f offset, float scale) {
        VrClientConfig.HudPlacement placement = VrClientConfig.hudPlacement();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (placement == VrClientConfig.HudPlacement.VIVECRAFT || player == null || mc.screen != null || !VR.hudOnHand()
                || !(player.getOffhandItem().getItem() instanceof GunItem)
                || VrClientConfig.hudExcluded(VrGunPose.gunId(player.getOffhandItem()))) {
            return guiPos;
        }
        VR.Aim hand = VR.offHandAim();
        if (hand == null || hand.dir().lengthSqr() < 1.0E-6) {
            return guiPos;
        }
        double worldScale = VR.worldScale();
        if (placement == VrClientConfig.HudPlacement.ELBOW) {
            return guiPos.subtract(hand.dir().normalize().scale(ELBOW_BACK * worldScale)).add(0.0, ELBOW_UP * worldScale, 0.0);
        }
        Vec3[] axes = VR.handAxes(false);
        return axes == null ? guiPos : onForearm(hand.pos(), axes, rotation, offset, scale * (float) worldScale);
    }

    /**
     * The HUD's X axis (along the hotbar) runs along the forearm, its Y axis is the controller's up and it faces the
     * inner side (towards the body). The anchor is the controller; {@code offset} puts the hotbar row on the forearm.
     */
    private static Vec3 onForearm(Vec3 handPos, Vec3[] axes, Matrix4f rotation, Vector3f offset, float hudScale) {
        Vec3 cx = axes[0];
        Vec3 cy = axes[1];
        Vec3 cz = axes[2];
        // The inner side of the off hand: the controller's +X for a left hand, -X for a right one.
        double inner = VR.leftHanded() ? -1.0 : 1.0;
        // Right-handed HUD frame: X = Y x Z, with Y = controller up and Z = towards the inner side.
        Vec3 hudZ = cx.scale(inner);
        Vec3 hudX = cy.cross(hudZ);
        rotation.set(new Vector4f((float) hudX.x, (float) hudX.y, (float) hudX.z, 0.0F),
                new Vector4f((float) cy.x, (float) cy.y, (float) cy.z, 0.0F),
                new Vector4f((float) hudZ.x, (float) hudZ.y, (float) hudZ.z, 0.0F),
                new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        double worldScale = VR.worldScale();
        // The hotbar's middle, in controller axes: back towards the elbow (+Z), inward, a little down.
        Vec3 target = cz.scale(FOREARM_BACK * worldScale)
                .add(hudZ.scale(FOREARM_INNER * worldScale))
                .add(cy.scale(-FOREARM_DOWN * worldScale));
        // offset is in HUD axes, before the HUD scale: the hotbar row is HOTBAR_ROW * hudScale below it.
        offset.set((float) target.dot(hudX), (float) (target.dot(cy) - HOTBAR_ROW * hudScale), (float) target.dot(hudZ));
        return handPos;
    }
}
