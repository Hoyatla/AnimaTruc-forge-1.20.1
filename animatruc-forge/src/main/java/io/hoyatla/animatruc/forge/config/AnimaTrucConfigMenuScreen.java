package io.hoyatla.animatruc.forge.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Minimal in-game config screen for AnimaTruc automatic preset toggles.
 */
public final class AnimaTrucConfigMenuScreen extends Screen {
    private final Screen parent;
    private Button performanceButton;
    private Button confidenceButton;

    public AnimaTrucConfigMenuScreen(Screen parent) {
        super(Component.literal("AnimaTruc Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(320, this.width - 40);
        int x = (this.width - width) / 2;
        int y = 40;
        int step = 24;

        this.addRenderableWidget(toggleButton(x, y, width, "Auto Detection", AnimaTrucPresetConfig.ENABLE_AUTO_DETECTION));
        y += step;
        this.addRenderableWidget(toggleButton(x, y, width, "Biped (2 legs)", AnimaTrucPresetConfig.ENABLE_BIPED));
        y += step;
        this.addRenderableWidget(toggleButton(x, y, width, "Hexapod (6 legs)", AnimaTrucPresetConfig.ENABLE_HEXAPOD));
        y += step;
        this.addRenderableWidget(toggleButton(x, y, width, "Octopod (8 legs)", AnimaTrucPresetConfig.ENABLE_OCTOPOD));
        y += step;
        this.addRenderableWidget(toggleButton(x, y, width, "Myriapod (many legs)", AnimaTrucPresetConfig.ENABLE_MYRIAPOD));
        y += step;
        this.addRenderableWidget(toggleButton(x, y, width, "Ground Raycast", AnimaTrucPresetConfig.ENABLE_GROUND_RAYCAST));
        y += step;

        this.performanceButton = Button.builder(Component.empty(), button -> {
            AnimaTrucPresetConfig.PerformanceMode current = AnimaTrucPresetConfig.PERFORMANCE_MODE.get();
            AnimaTrucPresetConfig.PerformanceMode[] values = AnimaTrucPresetConfig.PerformanceMode.values();
            int nextIndex = (current.ordinal() + 1) % values.length;
            AnimaTrucPresetConfig.PERFORMANCE_MODE.set(values[nextIndex]);
            refreshPerformanceLabel();
        }).bounds(x, y, width, 20).build();
        this.addRenderableWidget(this.performanceButton);
        refreshPerformanceLabel();
        y += step;

        this.confidenceButton = Button.builder(Component.empty(), button -> {
            double current = AnimaTrucPresetConfig.MIN_DETECTION_CONFIDENCE.get();
            double next = current >= 0.70d ? 0.40d : current >= 0.55d ? 0.70d : current >= 0.40d ? 0.55d : 0.40d;
            AnimaTrucPresetConfig.MIN_DETECTION_CONFIDENCE.set(next);
            refreshConfidenceLabel();
        }).bounds(x, y, width, 20).build();
        this.addRenderableWidget(this.confidenceButton);
        refreshConfidenceLabel();
        y += step + 10;

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(x, y, width, 20)
                .build());
    }

    @Override
    public void onClose() {
        AnimaTrucPresetConfig.SPEC.save();
        if (this.minecraft != null)
            this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Button toggleButton(int x, int y, int width, String label, ForgeConfigSpec.BooleanValue value) {
        Button button = Button.builder(Component.empty(), b -> {
            value.set(!value.get());
            b.setMessage(Component.literal(label + ": " + onOff(value.get())));
        }).bounds(x, y, width, 20).build();
        button.setMessage(Component.literal(label + ": " + onOff(value.get())));
        return button;
    }

    private void refreshPerformanceLabel() {
        this.performanceButton.setMessage(Component.literal("Performance Mode: " + AnimaTrucPresetConfig.PERFORMANCE_MODE.get().name()));
    }

    private void refreshConfidenceLabel() {
        this.confidenceButton.setMessage(Component.literal(
                "Min Detection Confidence: " + String.format(java.util.Locale.ROOT, "%.2f", AnimaTrucPresetConfig.MIN_DETECTION_CONFIDENCE.get())
        ));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
