package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.animation.AnimationChannel;
import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.animation.BoneAnimationTrack;
import io.hoyatla.animatruc.core.animation.ClipState;
import io.hoyatla.animatruc.core.animation.InterpolationMode;
import io.hoyatla.animatruc.core.animation.Interpolators;
import io.hoyatla.animatruc.core.animation.Keyframe;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatorInstanceTest {
    @Test
    void shouldThrottleAccordingToPolicy() {
        AdaptiveUpdatePolicy policy = new AdaptiveUpdatePolicy.Builder()
                .nearIntervalTicks(2)
                .build();
        AnimatorInstance instance = new AnimatorInstance(policy);
        instance.play(new ClipState(buildSimpleClip(), 1f, false));

        AnimatorContext context = new AnimatorContext(0f, true, false);

        assertFalse(instance.update(context, 1f).advancedThisTick());
        assertTrue(instance.update(context, 1f).advancedThisTick());
    }

    private static AnimationClip buildSimpleClip() {
        AnimationChannel<Vec3f> translation = new AnimationChannel<>(
                List.of(
                        new Keyframe<>(0f, Vec3f.ZERO, InterpolationMode.LINEAR),
                        new Keyframe<>(10f, new Vec3f(10f, 0f, 0f), InterpolationMode.LINEAR)
                ),
                Interpolators.VEC3,
                Vec3f.ZERO
        );
        AnimationChannel<Quatf> rotation = new AnimationChannel<>(
                List.of(new Keyframe<>(0f, Quatf.IDENTITY, InterpolationMode.LINEAR)),
                Interpolators.QUAT,
                Quatf.IDENTITY
        );
        AnimationChannel<Vec3f> scale = new AnimationChannel<>(
                List.of(new Keyframe<>(0f, Vec3f.ONE, InterpolationMode.LINEAR)),
                Interpolators.VEC3,
                Vec3f.ONE
        );

        BoneAnimationTrack track = new BoneAnimationTrack(translation, rotation, scale);

        return new AnimationClip("walk", 10f, true, Map.of("body", track));
    }
}
