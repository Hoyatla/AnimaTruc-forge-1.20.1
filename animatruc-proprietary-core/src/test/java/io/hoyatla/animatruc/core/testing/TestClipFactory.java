package io.hoyatla.animatruc.core.testing;

import io.hoyatla.animatruc.core.animation.AnimationChannel;
import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.animation.BoneAnimationTrack;
import io.hoyatla.animatruc.core.animation.InterpolationMode;
import io.hoyatla.animatruc.core.animation.Interpolators;
import io.hoyatla.animatruc.core.animation.Keyframe;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestClipFactory {
    private TestClipFactory() {}

    public static AnimationClip singleBoneConstant(String clipName, String boneName, Vec3f translation) {
        return multiBoneConstant(clipName, Map.of(boneName, translation));
    }

    public static AnimationClip multiBoneConstant(String clipName, Map<String, Vec3f> boneTranslations) {
        Map<String, BoneAnimationTrack> tracks = new HashMap<>();

        for (Map.Entry<String, Vec3f> entry : boneTranslations.entrySet()) {
            tracks.put(entry.getKey(), trackForConstantTranslation(entry.getValue()));
        }

        return new AnimationClip(clipName, 20f, true, tracks);
    }

    public static AnimationClip singleBoneRamp(String clipName, String boneName, Vec3f from, Vec3f to, float lengthTicks) {
        AnimationChannel<Vec3f> translation = new AnimationChannel<>(
                List.of(
                        new Keyframe<>(0f, from, InterpolationMode.LINEAR),
                        new Keyframe<>(lengthTicks, to, InterpolationMode.LINEAR)
                ),
                Interpolators.VEC3,
                from
        );
        AnimationChannel<Quatf> rotation = constantRotation();
        AnimationChannel<Vec3f> scale = constantScale();

        return new AnimationClip(clipName, lengthTicks, true, Map.of(boneName, new BoneAnimationTrack(translation, rotation, scale)));
    }

    private static BoneAnimationTrack trackForConstantTranslation(Vec3f translation) {
        AnimationChannel<Vec3f> translationChannel = new AnimationChannel<>(
                List.of(new Keyframe<>(0f, translation, InterpolationMode.LINEAR)),
                Interpolators.VEC3,
                translation
        );

        return new BoneAnimationTrack(translationChannel, constantRotation(), constantScale());
    }

    private static AnimationChannel<Quatf> constantRotation() {
        return new AnimationChannel<>(
                List.of(new Keyframe<>(0f, Quatf.IDENTITY, InterpolationMode.LINEAR)),
                Interpolators.QUAT,
                Quatf.IDENTITY
        );
    }

    private static AnimationChannel<Vec3f> constantScale() {
        return new AnimationChannel<>(
                List.of(new Keyframe<>(0f, Vec3f.ONE, InterpolationMode.LINEAR)),
                Interpolators.VEC3,
                Vec3f.ONE
        );
    }
}
