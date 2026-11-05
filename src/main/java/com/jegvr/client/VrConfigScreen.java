package com.jegvr.client;

import com.jegvr.config.VrClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The "Config" button of Forge's mod list. Edits {@link VrClientConfig} ({@code config/jeg_vr_compat-client.toml});
 * every change is saved at once, except the text field, saved when the screen closes.
 */
public final class VrConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;
    /** Rows above the "Done" button, one of them a gap. */
    private static final int ROWS = 10;
    private static final int MARGIN = 28;
    private static final int LABEL_GAP = 6;
    private static final int LIST_MAX_LENGTH = 512;

    private final Screen parent;
    private EditBox excludedGuns;
    private int top;

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
        int x = (this.width - BUTTON_WIDTH) / 2;
        // Rows shrink on small GUI sizes (never below the button height) so "Done" stays on screen.
        int row = Math.max(BUTTON_HEIGHT, Math.min(ROW, (this.height - MARGIN - BUTTON_HEIGHT - 4) / ROWS));
        this.top = Math.max(MARGIN, Math.min(this.height / 4, this.height - row * ROWS - BUTTON_HEIGHT - 4));
        int y = this.top;
        addRenderableWidget(CycleButton.<VrClientConfig.GrabZone>builder(VrConfigScreen::zoneName)
                .withValues(VrClientConfig.GrabZone.values())
                .withInitialValue(VrClientConfig.grabZone())
                .withTooltip(zone -> Tooltip.create(Component.translatable(zoneKey(zone) + ".tooltip")))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.grabZone"),
                        (button, zone) -> VrClientConfig.setGrabZone(zone)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.showGuides())
                .withTooltip(show -> Tooltip.create(Component.translatable("jeg_vr_compat.config.showGuides.tooltip")))
                .create(x, y + row, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.showGuides"),
                        (button, show) -> VrClientConfig.setShowGuides(show)));
        addRenderableWidget(CycleButton.<VrClientConfig.HudPlacement>builder(
                        placement -> Component.translatable(placementKey(placement)))
                .withValues(VrClientConfig.HudPlacement.values())
                .withInitialValue(VrClientConfig.hudPlacement())
                .withTooltip(placement -> Tooltip.create(Component.translatable(placementKey(placement) + ".tooltip")))
                .create(x, y + row * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.hudPlacement"),
                        (button, placement) -> VrClientConfig.setHudPlacement(placement)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.hudExclusion())
                .withTooltip(exclusion -> Tooltip.create(Component.translatable("jeg_vr_compat.config.hudExclusion.tooltip")))
                .create(x, y + row * 3, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.hudExclusion"),
                        (button, exclusion) -> VrClientConfig.setHudExclusion(exclusion)));
        Component listLabel = Component.translatable("jeg_vr_compat.config.hudExcludedGuns");
        int labelWidth = this.font.width(listLabel) + LABEL_GAP;
        this.excludedGuns = new EditBox(this.font, x + labelWidth, y + row * 4, BUTTON_WIDTH - labelWidth, BUTTON_HEIGHT, listLabel);
        this.excludedGuns.setMaxLength(LIST_MAX_LENGTH);
        this.excludedGuns.setValue(String.join(", ", VrClientConfig.hudExcludedGuns()));
        this.excludedGuns.setTooltip(Tooltip.create(Component.translatable("jeg_vr_compat.config.hudExcludedGuns.tooltip")));
        addRenderableWidget(this.excludedGuns);
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.mirrorLeftHand())
                .withTooltip(mirror -> Tooltip.create(Component.translatable("jeg_vr_compat.config.mirrorLeftHand.tooltip")))
                .create(x, y + row * 5, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.mirrorLeftHand"),
                        (button, mirror) -> VrClientConfig.setMirrorLeftHand(mirror)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.bowLaser())
                .withTooltip(laser -> Tooltip.create(Component.translatable("jeg_vr_compat.config.bowLaser.tooltip")))
                .create(x, y + row * 6, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.bowLaser"),
                        (button, laser) -> VrClientConfig.setBowLaser(laser)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.gunHud())
                .withTooltip(hud -> Tooltip.create(Component.translatable("jeg_vr_compat.config.gunHud.tooltip")))
                .create(x, y + row * 7, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.gunHud"),
                        (button, hud) -> VrClientConfig.setGunHud(hud)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.handsFollowAnimations())
                .withTooltip(follow -> Tooltip.create(Component.translatable("jeg_vr_compat.config.handsFollowAnimations.tooltip")))
                .create(x, y + row * 8, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("jeg_vr_compat.config.handsFollowAnimations"),
                        (button, follow) -> VrClientConfig.setHandsFollowAnimations(follow)));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, y + row * ROWS, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static Component zoneName(VrClientConfig.GrabZone zone) {
        return Component.translatable(zoneKey(zone));
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
        graphics.drawString(this.font, this.excludedGuns.getMessage(), (this.width - BUTTON_WIDTH) / 2,
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
