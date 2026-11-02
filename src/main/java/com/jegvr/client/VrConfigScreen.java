package com.jegvr.client;

import com.jegvr.config.VrClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.Locale;

/**
 * The "Config" button of Forge's mod list. Edits {@link VrClientConfig} ({@code config/jeg_vr_compat-client.toml});
 * every change is saved at once.
 */
public final class VrConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 250;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW = 24;

    private final Screen parent;

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
        int y = this.height / 4;
        addRenderableWidget(CycleButton.<VrClientConfig.GrabZone>builder(VrConfigScreen::zoneName)
                .withValues(VrClientConfig.GrabZone.values())
                .withInitialValue(VrClientConfig.grabZone())
                .withTooltip(zone -> Tooltip.create(Component.translatable(zoneKey(zone) + ".tooltip")))
                .create(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.grabZone"),
                        (button, zone) -> VrClientConfig.setGrabZone(zone)));
        addRenderableWidget(CycleButton.onOffBuilder(VrClientConfig.showGuides())
                .withTooltip(show -> Tooltip.create(Component.translatable("jeg_vr_compat.config.showGuides.tooltip")))
                .create(x, y + ROW, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("jeg_vr_compat.config.showGuides"),
                        (button, show) -> VrClientConfig.setShowGuides(show)));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, y + ROW * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static Component zoneName(VrClientConfig.GrabZone zone) {
        return Component.translatable(zoneKey(zone));
    }

    private static String zoneKey(VrClientConfig.GrabZone zone) {
        return "jeg_vr_compat.config.grabZone." + zone.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
