package io.hoyatla.animatruc.forge.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * In-game control surface for AnimaTruc runtime modules and automatic presets.
 */
public final class AnimaTrucConfigMenuScreen extends Screen {
    private final Screen parent;
    private Button performanceButton;
    private Button confidenceButton;

    public AnimaTrucConfigMenuScreen(Screen parent) {
        super(Component.literal("AnimaTruc Runtime"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int columnWidth = Math.min(260, Math.max(140, (this.width - 70) / 2));
        int leftX = (this.width - columnWidth * 2 - 10) / 2;
        int rightX = leftX + columnWidth + 10;
        int y = 42;
        int step = 24;

        int leftY = y;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Runtime", AnimaTrucGameplayConfig.ENABLE_RUNTIME));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Locomotion / Stamina", AnimaTrucGameplayConfig.ENABLE_LOCOMOTION));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Emotes / Wheel", AnimaTrucGameplayConfig.ENABLE_EMOTES));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Sound Perception", AnimaTrucGameplayConfig.ENABLE_PERCEPTION));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Combat Feedback", AnimaTrucGameplayConfig.ENABLE_COMBAT_FEEDBACK));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Explosion Feedback", AnimaTrucGameplayConfig.ENABLE_EXPLOSION_FEEDBACK));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Weight / Fatigue", AnimaTrucGameplayConfig.ENABLE_WEIGHT));
        leftY += step;
        this.addRenderableWidget(toggleButton(leftX, leftY, columnWidth, "Input / UI Movement", AnimaTrucGameplayConfig.ENABLE_INPUT_UI));

        int rightY = y;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Auto Detection", AnimaTrucPresetConfig.ENABLE_AUTO_DETECTION));
        rightY += step;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Biped (2 legs)", AnimaTrucPresetConfig.ENABLE_BIPED));
        rightY += step;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Hexapod (6 legs)", AnimaTrucPresetConfig.ENABLE_HEXAPOD));
        rightY += step;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Octopod (8 legs)", AnimaTrucPresetConfig.ENABLE_OCTOPOD));
        rightY += step;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Myriapod (many legs)", AnimaTrucPresetConfig.ENABLE_MYRIAPOD));
        rightY += step;
        this.addRenderableWidget(toggleButton(rightX, rightY, columnWidth, "Ground Raycast", AnimaTrucPresetConfig.ENABLE_GROUND_RAYCAST));
        rightY += step;

        this.performanceButton = Button.builder(Component.empty(), button -> {
            AnimaTrucPresetConfig.PerformanceMode current = AnimaTrucPresetConfig.PERFORMANCE_MODE.get();
            AnimaTrucPresetConfig.PerformanceMode[] values = AnimaTrucPresetConfig.PerformanceMode.values();
            int nextIndex = (current.ordinal() + 1) % values.length;
            AnimaTrucPresetConfig.PERFORMANCE_MODE.set(values[nextIndex]);
            refreshPerformanceLabel();
        }).bounds(rightX, rightY, columnWidth, 20).build();
        this.addRenderableWidget(this.performanceButton);
        refreshPerformanceLabel();
        rightY += step;

        this.confidenceButton = Button.builder(Component.empty(), button -> {
            double current = AnimaTrucPresetConfig.MIN_DETECTION_CONFIDENCE.get();
            double next = current >= 0.70d ? 0.40d : current >= 0.55d ? 0.70d : current >= 0.40d ? 0.55d : 0.40d;
            AnimaTrucPresetConfig.MIN_DETECTION_CONFIDENCE.set(next);
            refreshConfidenceLabel();
        }).bounds(rightX, rightY, columnWidth, 20).build();
        this.addRenderableWidget(this.confidenceButton);
        refreshConfidenceLabel();

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds((this.width - 160) / 2, this.height - 32, 160, 20)
                .build());
    }

    @Override
    public void onClose() {
        AnimaTrucGameplayConfig.SPEC.save();
        AnimaTrucPresetConfig.SPEC.save();
        if (this.minecraft != null)
            this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawString(this.font, "Gameplay Modules", Math.max(10, (this.width - 530) / 2), 30, 0xA7E7FF, false);
        graphics.drawString(this.font, "Animation Presets", Math.max(10, (this.width - 530) / 2) + 270, 30, 0xA7E7FF, false);
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
