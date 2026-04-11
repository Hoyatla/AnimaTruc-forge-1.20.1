package io.hoyatla.animatruc.core.animation;

import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

public final class BoneAnimationTrack {
    private final AnimationChannel<Vec3f> translationChannel;
    private final AnimationChannel<Quatf> rotationChannel;
    private final AnimationChannel<Vec3f> scaleChannel;

    public BoneAnimationTrack(
            AnimationChannel<Vec3f> translationChannel,
            AnimationChannel<Quatf> rotationChannel,
            AnimationChannel<Vec3f> scaleChannel) {
        this.translationChannel = Objects.requireNonNull(translationChannel, "translationChannel");
        this.rotationChannel = Objects.requireNonNull(rotationChannel, "rotationChannel");
        this.scaleChannel = Objects.requireNonNull(scaleChannel, "scaleChannel");
    }

    public Transform sample(float tick, float clipLengthTicks, boolean looping) {
        Vec3f translation = this.translationChannel.sample(tick, clipLengthTicks, looping);
        Quatf rotation = this.rotationChannel.sample(tick, clipLengthTicks, looping);
        Vec3f scale = this.scaleChannel.sample(tick, clipLengthTicks, looping);

        return new Transform(translation, rotation, scale);
    }
}
