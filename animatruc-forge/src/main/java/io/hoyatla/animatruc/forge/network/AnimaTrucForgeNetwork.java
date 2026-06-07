package io.hoyatla.animatruc.forge.network;

import io.hoyatla.animatruc.core.gameplay.CameraFeedback;
import io.hoyatla.animatruc.forge.AnimaTrucForgeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AnimaTrucForgeNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;
    private static int packetId;

    private AnimaTrucForgeNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(AnimaTrucForgeMod.MOD_ID, "gameplay"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        channel.registerMessage(packetId++, AnimaTrucFeedbackPacket.class, AnimaTrucFeedbackPacket::encode, AnimaTrucFeedbackPacket::decode, AnimaTrucFeedbackPacket::handle);
    }

    public static void sendFeedback(ServerPlayer player, CameraFeedback feedback) {
        if (channel == null || player == null || feedback == null || !feedback.active())
            return;

        channel.send(PacketDistributor.PLAYER.with(() -> player), new AnimaTrucFeedbackPacket(
                feedback.shake(),
                feedback.rollDegrees(),
                feedback.blurTicks(),
                feedback.vignette(),
                feedback.soundMuffle()
        ));
    }
}
