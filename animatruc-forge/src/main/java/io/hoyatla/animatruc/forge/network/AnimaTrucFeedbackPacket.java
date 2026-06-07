package io.hoyatla.animatruc.forge.network;

import io.hoyatla.animatruc.forge.client.AnimaTrucClientFeedback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AnimaTrucFeedbackPacket(float shake, float rollDegrees, int blurTicks, float vignette, float soundMuffle) {
    public static void encode(AnimaTrucFeedbackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.shake);
        buffer.writeFloat(packet.rollDegrees);
        buffer.writeVarInt(packet.blurTicks);
        buffer.writeFloat(packet.vignette);
        buffer.writeFloat(packet.soundMuffle);
    }

    public static AnimaTrucFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new AnimaTrucFeedbackPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(AnimaTrucFeedbackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AnimaTrucClientFeedback.apply(packet)));
        context.setPacketHandled(true);
    }
}
