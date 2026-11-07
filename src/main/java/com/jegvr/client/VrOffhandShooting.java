package com.jegvr.client;

import com.jegvr.compat.jeg.JegKeys;
import com.jegvr.compat.VR;
import com.jegvr.mixin.input.ItemCooldownInstanceAccessor;
import com.jegvr.mixin.input.ItemCooldownsAccessor;
import com.jegvr.mixin.input.ShootingHandlerAccessor;
import com.jegvr.network.C2SVrPreFireOffhand;
import com.jegvr.network.C2SVrShootOffhand;
import com.jegvr.network.VrNetwork;
import com.jegvr.server.VrOffhand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ttv.migami.jeg.client.handler.GunRenderingHandler;
import ttv.migami.jeg.client.handler.ShootingHandler;
import ttv.migami.jeg.common.FireMode;
import ttv.migami.jeg.common.Gun;
import ttv.migami.jeg.common.ReloadType;
import ttv.migami.jeg.compat.PlayerReviveHelper;
import ttv.migami.jeg.event.GunFireEvent;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.IAttachment;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageBurnPlayer;
import ttv.migami.jeg.network.message.C2SMessageOverheat;
import ttv.migami.jeg.util.GunEnchantmentHelper;
import ttv.migami.jeg.util.GunModifierHelper;

/**
 * Client side of the off-hand gun (dual wield, S.5). With a gun in the off hand, in VR and on a server with this
 * addon, that hand's trigger (Vivecraft's teleport key, see {@code KeyMappingVRMixin}) or {@link VrKeys#FIRE_OFFHAND}
 * fires it through {@link C2SVrShootOffhand}, under the same rules as JEG's main-hand trigger. This follows
 * {@code ShootingHandler.onPostClientTick} and {@code fire} step by step, with its own state: fire modes (bursts, one
 * shot per press), the wind-up before firing, charging by holding (bows), overheating and the cooldown of the gun. The
 * trigger keeps its normal role while the main hand holds a Vivecraft bow (drawn with that trigger).
 */
public final class VrOffhandShooting {
    /** JEG's fixed values ({@code ShootingHandler}). */
    private static final int OVERHEAT_COOLDOWN = 80;
    private static final int PRE_FIRE_SOUND_TICK = 2;
    private static final int MIN_HOLD_TO_FIRE = 5;
    private static final int SWAP_SETTLE_TICKS = 2;

    private static boolean triggerTakenOver;
    /** The trigger as JEG sees its shoot key: pressed, until released or until JEG would release it after a shot. */
    private static boolean trigger;
    private static boolean rawWasDown;
    /** JEG keeps cooldowns per item on the player, shared with the main hand; this hand has its own. */
    private static int cooldown;
    private static Item cooldownItem;
    private static int fireTimer;
    private static int holdFire;
    private static int overheatTimer;
    private static boolean previouslyPressed;
    private static boolean overheated;
    private static int burstCount;
    private static Item knownMain;
    private static Item knownOff;
    private static int settlingTicks;

    private VrOffhandShooting() {
    }

    /** True while the off-hand trigger fires the off-hand gun instead of teleporting. */
    public static boolean triggerTakenOver() {
        return triggerTakenOver;
    }

    public static boolean triggerHeld() {
        return trigger;
    }

    public static int fireTimer() {
        return fireTimer;
    }

    public static int holdFire() {
        return holdFire;
    }

    public static int overheatTimer() {
        return overheatTimer;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        triggerTakenOver = player != null && VrOffhand.canDualWield(player.getOffhandItem())
                && !VR.isBow(player.getMainHandItem()) && VR.active() && VrAim.serverHasAddon();
        if (player != null) {
            trackHandSwap(player);
            // The server needs to know where the gun points for its flashlight or laser pointer, not only when it fires.
            if (triggerTakenOver && Gun.hasAttachmentEquipped(player.getOffhandItem(), IAttachment.Type.SPECIAL)) {
                VrAim.offHandShotRotation();
            }
        }
        if (player == null || mc.screen != null || mc.getOverlay() != null) {
            trigger = false;
            rawWasDown = false;
            return;
        }
        readTrigger();
        if (PlayerReviveHelper.isBleeding(player)) {
            return;
        }
        if (!trigger) {
            fireTimer = 0;
            if (!previouslyPressed) {
                holdFire = 0;
            } else if (holdFire > 0) {
                holdFire--;
            }
            if (overheatTimer > 0) {
                overheatTimer--;
            }
        }
        if (overheated && overheatTimer <= 0) {
            overheated = false;
        }
        if (player.isUnderWater()) {
            overheatTimer = 0;
            overheated = false;
        }
        if (overheated) {
            trigger = false;
        }
        ItemStack stack = player.getOffhandItem();
        if (triggerTakenOver && stack.getItem() instanceof GunItem gunItem) {
            tickGun(player, stack, gunItem.getModifiedGun(stack));
        } else {
            previouslyPressed = false;
        }
    }

    /**
     * Each hand keeps its own heat and cooldown, so a gun that changes hands takes them along; otherwise swapping hands
     * would cool it at once. The two hands' slots may arrive from the server a tick apart, so a change that is not yet a
     * swap is given a moment to become one.
     */
    private static void trackHandSwap(LocalPlayer player) {
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        if (main == knownMain && off == knownOff) {
            settlingTicks = 0;
            return;
        }
        boolean swapped = main == knownOff && off == knownMain;
        if (swapped || ++settlingTicks > SWAP_SETTLE_TICKS || !VR.active() || !VrAim.serverHasAddon()) {
            if (swapped && VR.active() && VrAim.serverHasAddon()) {
                swapStates(player, main, off);
            }
            knownMain = main;
            knownOff = off;
            settlingTicks = 0;
        }
    }

    private static void swapStates(LocalPlayer player, Item nowMain, Item nowOff) {
        ShootingHandlerAccessor mainHand = (ShootingHandlerAccessor) ShootingHandler.get();
        int mainHeat = mainHand.jegvr$getOverheatTimer();
        boolean mainOverheated = mainHand.jegvr$isOverheated();
        mainHand.jegvr$setOverheatTimer(overheatTimer);
        mainHand.jegvr$setOverheated(overheated);
        overheatTimer = mainHeat;
        overheated = mainOverheated;

        int offLeft = onCooldown(nowMain) ? cooldown : 0;
        int mainLeft = mainCooldownLeft(player, nowOff);
        player.getCooldowns().removeCooldown(nowOff);
        if (offLeft > 0) {
            player.getCooldowns().addCooldown(nowMain, offLeft);
        }
        startCooldown(nowOff, mainLeft);
    }

    private static int mainCooldownLeft(LocalPlayer player, Item item) {
        ItemCooldownsAccessor cooldowns = (ItemCooldownsAccessor) player.getCooldowns();
        Object instance = cooldowns.jegvr$getCooldowns().get(item);
        return instance == null ? 0
                : Math.max(0, ((ItemCooldownInstanceAccessor) instance).jegvr$getEndTime() - cooldowns.jegvr$getTickCount());
    }

    private static void readTrigger() {
        boolean raw = triggerTakenOver && (VrKeys.FIRE_OFFHAND.isDown() || VrTriggers.offTriggerDown());
        if (raw && !rawWasDown) {
            trigger = true;
        } else if (!raw) {
            trigger = false;
        }
        rawWasDown = raw;
    }

    private static void tickGun(LocalPlayer player, ItemStack stack, Gun gun) {
        Gun.General general = gun.getGeneral();
        FireMode mode = general.getFireMode();
        boolean creative = player.isCreative();
        if (general.getOverheatTimer() == 0) {
            overheated = false;
        }
        CompoundTag tag = stack.getTag();
        // JEG reloads the main-hand gun when its trigger is pulled empty (ShootingHandler); same for this hand.
        if (trigger && !Gun.hasAmmo(stack) && !creative && tag != null && tag.getInt(JegKeys.AMMO_COUNT) <= 0
                && !ReloadType.INVENTORY_FED.equals(gun.getReloads().getReloadType())) {
            VrDualReload.reloadOffhand(player);
        }
        if (!trigger && mode == FireMode.RELEASE_FIRE && holdFire > MIN_HOLD_TO_FIRE && previouslyPressed) {
            fire(player, stack, gun);
        }
        if ((trigger || (burstCount > 0 && mode == FireMode.BURST)) && mode != FireMode.RELEASE_FIRE
                && mode != FireMode.SEMI_AUTO && mode != FireMode.PULSE) {
            fire(player, stack, gun);
            if (mode != FireMode.AUTOMATIC && mode != FireMode.BURST) {
                trigger = false;
            }
        }
        if (!Gun.hasAmmo(stack) && !creative) {
            trigger = false;
            burstCount = 0;
        }
        if (trigger) {
            tickHeld(player, stack, gun);
        }
    }

    /** The trigger is held: heat, charge and wind-up. */
    private static void tickHeld(LocalPlayer player, ItemStack stack, Gun gun) {
        Gun.General general = gun.getGeneral();
        FireMode mode = general.getFireMode();
        Item item = stack.getItem();
        if (general.getOverheatTimer() != 0 && overheatTimer < general.getOverheatTimer()) {
            overheatTimer++;
            if (overheatTimer >= general.getOverheatTimer()) {
                startCooldown(item, OVERHEAT_COOLDOWN);
                if (stack.is(ModItems.FLAMETHROWER.get())) {
                    PacketHandler.getPlayChannel().sendToServer(new C2SMessageBurnPlayer());
                }
                overheated = true;
                trigger = false;
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageOverheat());
            }
        }
        if (general.getMaxHoldFire() != 0 && holdFire < general.getMaxHoldFire() && !onCooldown(item)) {
            previouslyPressed = true;
            holdFire++;
        }
        if (general.getFireTimer() != 0) {
            if (fireTimer < general.getFireTimer() && !onCooldown(item)) {
                if (fireTimer == PRE_FIRE_SOUND_TICK) {
                    VrNetwork.CHANNEL.sendToServer(new C2SVrPreFireOffhand());
                }
                fireTimer++;
                if (player.isUnderWater()) {
                    fireTimer++;
                }
            } else {
                fire(player, stack, gun);
                if (mode == FireMode.SEMI_AUTO || mode == FireMode.PULSE) {
                    trigger = false;
                    fireTimer = 0;
                }
            }
        } else {
            if (mode != FireMode.RELEASE_FIRE) {
                fire(player, stack, gun);
            }
            if (mode == FireMode.SEMI_AUTO) {
                trigger = false;
            }
        }
    }

    static boolean onCooldown(Item item) {
        return cooldown > 0 && item == cooldownItem;
    }

    static void startCooldown(Item item, int ticks) {
        cooldown = ticks;
        cooldownItem = item;
    }

    private static void fire(LocalPlayer player, ItemStack stack, Gun gun) {
        if ((!Gun.hasAmmo(stack) && !player.isCreative()) || player.isSpectator()
                || player.getUseItem().getItem() instanceof ShieldItem) {
            return;
        }
        Gun.General general = gun.getGeneral();
        float charge = general.getMaxHoldFire() > 0 ? Math.min(1.0F, (float) holdFire / general.getMaxHoldFire()) : 0.0F;
        holdFire = 0;
        previouslyPressed = false;
        Item item = stack.getItem();
        if (onCooldown(item) || (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1)) {
            return;
        }
        float[] rotation = VrAim.offHandShotRotation();
        if (rotation == null) {
            return;
        }
        // Same rate JEG uses for the main hand (ShootingHandler.fire / ShootTracker.putCooldown).
        startCooldown(item, Math.max(1, GunModifierHelper.getModifiedRate(stack, GunEnchantmentHelper.getRate(stack, gun))));
        if (general.getFireMode() == FireMode.BURST) {
            if (burstCount == 1) {
                startCooldown(item, general.getBurstDelay());
            }
            burstCount = burstCount <= 0 ? general.getBurstAmount() - 1 : burstCount - 1;
        }
        VrNetwork.CHANNEL.sendToServer(new C2SVrShootOffhand(rotation[0], rotation[1], charge));

        // What JEG's client does for a main-hand shot, without posting the event (our main-hand listeners would
        // treat it as a main-hand shot): the constructor starts the shoot animation of this stack, then the flash.
        new GunFireEvent.Post(player, stack);
        if (gun.getDisplay().getFlash() != null) {
            GunRenderingHandler.get().showMuzzleFlashForPlayer(player.getId());
        }
        VrHands.markShot(InteractionHand.OFF_HAND);
        pushBack(player, general);

        float angle = general.getRecoilAngle() * (1.0F - GunModifierHelper.getRecoilModifier(stack));
        VR.haptic(false, 0.06F, Mth.clamp(angle / 10.0F, 0.2F, 1.0F));
    }

    /**
     * The push JEG gives the shooter ({@code GunEventBus.postShoot} and {@code recoil}), halved when crouching on solid
     * ground. It only counts on the client, in the event this hand does not post, and it follows this gun's barrel.
     */
    private static void pushBack(LocalPlayer player, Gun.General general) {
        VR.Aim aim = VrPrecision.aim(InteractionHand.OFF_HAND, VR.offHandAim());
        float force = general.getShooterPushback();
        if (aim == null || force == 0.0F) {
            return;
        }
        if (player.isCrouching() && player.level().getBlockState(player.getOnPos()).isSolid()) {
            force /= 2.0F;
        }
        Vec3 push = aim.dir().normalize().scale(force);
        player.push(push.x, push.y, push.z);
        player.fallDistance = 0.0F;
    }
}
