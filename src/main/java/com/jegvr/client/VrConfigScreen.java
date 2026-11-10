package com.jegvr.client;

import com.jegvr.config.VrClientConfig;
import com.jegvr.config.VrRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The "Config" button of Forge's mod list. Edits {@link VrClientConfig} ({@code config/jeg_vr_compat-client.toml});
 * every change is saved at once, except the text field, saved when the screen closes.
 */
public final class VrConfigScreen extends Screen {
    /** Widest a button gets; narrower when two columns do not fit (Vivecraft's GUI panel is small). */
    private static final int MAX_BUTTON_WIDTH = 250;
    private static final int MIN_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;
    private static final int COLUMN_GAP = 10;
    private static final int TITLE_SPACE = 24;
    private static final int BOTTOM_SPACE = 8;
    private static final int LABEL_GAP = 6;
    private static final int LIST_MAX_LENGTH = 512;

    private final Screen parent;
    private EditBox excludedGuns;
    private int top;
    private int listLabelX;

    private VrConfigScreen(Screen parent) {
        super(Component.translatable("jeg_vr_compat.config.title"));
        this.parent = parent;
    }

    /** Client only: hooks this screen to the mod list. */
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new VrConfigScreen(parent)));
    }

    @Override
    protected void init() {
        // Two columns, so everything fits in Vivecraft's GUI panel (about 6 rows high at the usual GUI scale).
        // One column only when the screen is too narrow for two.
        int twoColumnWidth = Math.min(MAX_BUTTON_WIDTH, (this.width - COLUMN_GAP * 3) / 2);
        boolean twoColumns = twoColumnWidth >= MIN_BUTTON_WIDTH;
        int width = twoColumns ? twoColumnWidth : Math.min(MAX_BUTTON_WIDTH, this.width - COLUMN_GAP * 2);
        List<AbstractWidget> left = new ArrayList<>();
        List<AbstractWidget> right = new ArrayList<>();
        VrRules.Rules rules = VrRulesClient.rules();

        left.add(CycleButton.<VrClientConfig.GrabZone>builder(VrConfigScreen::zoneName)
                .withValues(VrClientConfig.GrabZone.values())
                .withInitialValue(VrClientConfig.grabZone())
                .withTooltip(zone -> Tooltip.create(Component.translatable(zoneKey(zone) + ".tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.grabZone"),
                        (button, zone) -> VrClientConfig.setGrabZone(zone)));
        left.add(CycleButton.onOffBuilder(VrClientConfig.showGuides())
                .withTooltip(show -> Tooltip.create(Component.translatable("jeg_vr_compat.config.showGuides.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.showGuides"),
                        (button, show) -> VrClientConfig.setShowGuides(show)));
        left.add(CycleButton.onOffBuilder(VrClientConfig.showSprites())
                .withTooltip(show -> Tooltip.create(Component.translatable("jeg_vr_compat.config.showSprites.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.showSprites"),
                        (button, show) -> VrClientConfig.setShowSprites(show)));
        boolean reloadDecided = rules.reloadWithBothGuns() != VrRules.Toggle.PLAYER;
        left.add(serverDecides(CycleButton.onOffBuilder(
                        reloadDecided ? VrClientConfig.reloadWithBothGuns() : VrClientConfig.ownReloadWithBothGuns())
                .withTooltip(allow -> tooltip("jeg_vr_compat.config.reloadWithBothGuns.tooltip", reloadDecided))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.reloadWithBothGuns"),
                        (button, allow) -> VrClientConfig.setReloadWithBothGuns(allow)), reloadDecided));
        boolean throwDecided = rules.throwMode() != VrRules.Throw.PLAYER;
        left.add(serverDecides(CycleButton.<VrClientConfig.ThrowMode>builder(
                        mode -> Component.translatable(throwModeKey(mode)))
                .withValues(VrClientConfig.ThrowMode.values())
                .withInitialValue(throwDecided ? VrClientConfig.throwMode() : VrClientConfig.ownThrowMode())
                .withTooltip(mode -> tooltip(throwModeKey(mode) + ".tooltip", throwDecided))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.throwMode"),
                        (button, mode) -> VrClientConfig.setThrowMode(mode)), throwDecided));
        left.add(CycleButton.onOffBuilder(VrClientConfig.mirrorLeftHand())
                .withTooltip(mirror -> Tooltip.create(Component.translatable("jeg_vr_compat.config.mirrorLeftHand.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.mirrorLeftHand"),
                        (button, mirror) -> VrClientConfig.setMirrorLeftHand(mirror)));
        boolean laserDecided = rules.bowLaser() != VrRules.Toggle.PLAYER;
        left.add(serverDecides(CycleButton.onOffBuilder(
                        laserDecided ? VrClientConfig.bowLaser() : VrClientConfig.ownBowLaser())
                .withTooltip(laser -> tooltip("jeg_vr_compat.config.bowLaser.tooltip", laserDecided))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.bowLaser"),
                        (button, laser) -> VrClientConfig.setBowLaser(laser)), laserDecided));

        right.add(CycleButton.<VrClientConfig.HudPlacement>builder(
                        placement -> Component.translatable(placementKey(placement)))
                .withValues(VrClientConfig.HudPlacement.values())
                .withInitialValue(VrClientConfig.hudPlacement())
                .withTooltip(placement -> Tooltip.create(Component.translatable(placementKey(placement) + ".tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.hudPlacement"),
                        (button, placement) -> VrClientConfig.setHudPlacement(placement)));
        right.add(CycleButton.onOffBuilder(VrClientConfig.hudExclusion())
                .withTooltip(exclusion -> Tooltip.create(Component.translatable("jeg_vr_compat.config.hudExclusion.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.hudExclusion"),
                        (button, exclusion) -> VrClientConfig.setHudExclusion(exclusion)));
        Component listLabel = Component.translatable("jeg_vr_compat.config.hudExcludedGuns");
        int labelWidth = this.font.width(listLabel) + LABEL_GAP;
        this.excludedGuns = new EditBox(this.font, 0, 0, width - labelWidth, BUTTON_HEIGHT, listLabel);
        this.excludedGuns.setMaxLength(LIST_MAX_LENGTH);
        this.excludedGuns.setValue(String.join(", ", VrClientConfig.hudExcludedGuns()));
        this.excludedGuns.setTooltip(Tooltip.create(Component.translatable("jeg_vr_compat.config.hudExcludedGuns.tooltip")));
        right.add(this.excludedGuns);
        right.add(CycleButton.onOffBuilder(VrClientConfig.gunHud())
                .withTooltip(hud -> Tooltip.create(Component.translatable("jeg_vr_compat.config.gunHud.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.gunHud"),
                        (button, hud) -> VrClientConfig.setGunHud(hud)));
        right.add(CycleButton.onOffBuilder(VrClientConfig.handsFollowAnimations())
                .withTooltip(follow -> Tooltip.create(Component.translatable("jeg_vr_compat.config.handsFollowAnimations.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.handsFollowAnimations"),
                        (button, follow) -> VrClientConfig.setHandsFollowAnimations(follow)));
        right.add(CycleButton.<VrClientConfig.GripHint>builder(hint -> Component.translatable(gripHintKey(hint)))
                .withValues(VrClientConfig.GripHint.values())
                .withInitialValue(VrClientConfig.gripHint())
                .withTooltip(hint -> Tooltip.create(Component.translatable(gripHintKey(hint) + ".tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.gripHint"),
                        (button, hint) -> VrClientConfig.setGripHint(hint)));
        right.add(new GripRangeSlider(width));
        left.add(CycleButton.onOffBuilder(VrClientConfig.lockHotbarInPrecision())
                .withTooltip(lock -> Tooltip.create(Component.translatable("jeg_vr_compat.config.lockHotbarInPrecision.tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.lockHotbarInPrecision"),
                        (button, lock) -> VrClientConfig.setLockHotbarInPrecision(lock)));
        right.add(CycleButton.<VrClientConfig.EyeFlash>builder(flash -> Component.translatable(eyeFlashKey(flash)))
                .withValues(VrClientConfig.EyeFlash.values())
                .withInitialValue(VrClientConfig.eyeFlash())
                .withTooltip(flash -> Tooltip.create(Component.translatable(eyeFlashKey(flash) + ".tooltip")))
                .create(0, 0, width, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.eyeFlash"),
                        (button, flash) -> VrClientConfig.setEyeFlash(flash)));

        if (!twoColumns) {
            left.addAll(right);
            right.clear();
        }
        int rows = Math.max(left.size(), right.size());
        // Rows shrink on short screens (never below the button height); "Done" goes one row below the last.
        int row = Math.max(BUTTON_HEIGHT,
                Math.min(ROW, (this.height - TITLE_SPACE - BOTTOM_SPACE - BUTTON_HEIGHT) / (rows + 1)));
        int contentHeight = row * rows + BUTTON_HEIGHT;
        this.top = Math.max(TITLE_SPACE, Math.min(this.height / 6, (this.height - contentHeight) / 2));
        int totalWidth = twoColumns ? width * 2 + COLUMN_GAP : width;
        int leftX = (this.width - totalWidth) / 2;
        place(left, leftX, row);
        place(right, leftX + width + COLUMN_GAP, row);
        // The list's label is drawn to the left of its text field (render).
        this.listLabelX = this.excludedGuns.getX();
        this.excludedGuns.setX(this.excludedGuns.getX() + labelWidth);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds((this.width - width) / 2, this.top + row * rows, width, BUTTON_HEIGHT).build());
    }

    private void place(List<AbstractWidget> column, int x, int row) {
        for (int i = 0; i < column.size(); i++) {
            AbstractWidget widget = column.get(i);
            widget.setPosition(x, this.top + row * i);
            addRenderableWidget(widget);
        }
    }

    /** F10.4: how close the free hand must come for the grip particles (10 to 100 cm, in 5 cm steps). */
    private static final class GripRangeSlider extends AbstractSliderButton {
        private static final double MIN = 0.1;
        private static final double MAX = 1.0;
        private static final double STEP = 0.05;

        GripRangeSlider(int width) {
            super(0, 0, width, BUTTON_HEIGHT, Component.empty(),
                    (VrClientConfig.gripHintRange() - MIN) / (MAX - MIN));
            setTooltip(Tooltip.create(Component.translatable("jeg_vr_compat.config.gripHintRange.tooltip")));
            updateMessage();
        }

        private double metres() {
            return Math.round((MIN + this.value * (MAX - MIN)) / STEP) * STEP;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("jeg_vr_compat.config.gripHintRange",
                    Math.round(metres() * 100.0)));
        }

        @Override
        protected void applyValue() {
            VrClientConfig.setGripHintRange(metres());
        }
    }

    /** S.8: an option the server decides shows the server's value and cannot be changed here. */
    private static <T> CycleButton<T> serverDecides(CycleButton<T> button, boolean decided) {
        button.active = !decided;
        return button;
    }

    private static Tooltip tooltip(String key, boolean decided) {
        Component text = Component.translatable(key);
        return Tooltip.create(decided
                ? Component.translatable("jeg_vr_compat.rules.decided").append("\n").append(text)
                : text);
    }

    private static Component zoneName(VrClientConfig.GrabZone zone) {
        return Component.translatable(zoneKey(zone));
    }

    private static String throwModeKey(VrClientConfig.ThrowMode mode) {
        return "jeg_vr_compat.config.throwMode." + mode.name().toLowerCase(Locale.ROOT);
    }

    private static String gripHintKey(VrClientConfig.GripHint hint) {
        return "jeg_vr_compat.config.gripHint." + hint.name().toLowerCase(Locale.ROOT);
    }

    private static String eyeFlashKey(VrClientConfig.EyeFlash flash) {
        return "jeg_vr_compat.config.eyeFlash." + flash.name().toLowerCase(Locale.ROOT);
    }

    private static String placementKey(VrClientConfig.HudPlacement placement) {
        return "jeg_vr_compat.config.hudPlacement." + placement.name().toLowerCase(Locale.ROOT);
    }

    private static String zoneKey(VrClientConfig.GrabZone zone) {
        return "jeg_vr_compat.config.grabZone." + zone.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top - 16, 0xFFFFFF);
        graphics.drawString(this.font, this.excludedGuns.getMessage(), this.listLabelX,
                this.excludedGuns.getY() + (BUTTON_HEIGHT - this.font.lineHeight) / 2, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        this.excludedGuns.tick();
    }

    @Override
    public void removed() {
        List<String> guns = Arrays.stream(this.excludedGuns.getValue().split(","))
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .filter(id -> !id.isEmpty())
                .toList();
        VrClientConfig.setHudExcludedGuns(guns);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
