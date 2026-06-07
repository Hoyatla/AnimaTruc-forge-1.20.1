package io.hoyatla.animatruc.forge.client;

import io.hoyatla.animatruc.forge.network.AnimaTrucFeedbackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AnimaTrucClientFeedback {
    private static float shake;
    private static float rollDegrees;
    private static int blurTicks;
    private static float vignette;
    private static float soundMuffle;

    private AnimaTrucClientFeedback() {
    }

    public static void apply(AnimaTrucFeedbackPacket packet) {
        shake = Math.max(shake, packet.shake());
        rollDegrees += packet.rollDegrees();
        blurTicks = Math.max(blurTicks, packet.blurTicks());
        vignette = Math.max(vignette, packet.vignette());
        soundMuffle = Math.max(soundMuffle, packet.soundMuffle());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        shake *= 0.88f;
        rollDegrees *= 0.82f;
        vignette *= 0.93f;
        soundMuffle *= 0.92f;
        if (blurTicks > 0)
            blurTicks--;

        if (shake < 0.003f)
            shake = 0f;
        if (Math.abs(rollDegrees) < 0.02f)
            rollDegrees = 0f;
        if (vignette < 0.003f)
            vignette = 0f;
        if (soundMuffle < 0.003f)
            soundMuffle = 0f;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0f && rollDegrees == 0f)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        float randomYaw = (minecraft.level == null ? 0f : (minecraft.level.random.nextFloat() - 0.5f) * shake * 1.8f);
        float randomPitch = (minecraft.level == null ? 0f : (minecraft.level.random.nextFloat() - 0.5f) * shake * 1.8f);
        event.setYaw(event.getYaw() + randomYaw);
        event.setPitch(event.getPitch() + randomPitch);
        event.setRoll(event.getRoll() + rollDegrees);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!VanillaGuiOverlay.VIGNETTE.id().equals(event.getOverlay().id()))
            return;

        float alpha = Math.max(vignette, blurTicks > 0 ? Math.min(0.45f, blurTicks / 120f) : 0f);
        if (alpha <= 0f)
            return;

        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int a = Math.min(180, Math.max(0, (int)(alpha * 180f)));
        int color = (a << 24) | 0x1A1A24;
        graphics.fill(0, 0, width, height, color);
    }

    public static float soundMuffle() {
        return soundMuffle;
    }
}
