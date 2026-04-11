package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

/**
 * Adds a lightweight sinusoidal breathing layer on one bone.
 */
public final class BreathingModifier implements AnimationModifier {
    private static final float TWO_PI = (float)(Math.PI * 2d);

    private final String boneName;
    private final float translationAmplitudeY;
    private final float scaleAmplitude;
    private final float frequencyHz;
    private float timeSeconds;

    public BreathingModifier(String boneName, float translationAmplitudeY, float scaleAmplitude, float frequencyHz) {
        this.boneName = boneName;
        this.translationAmplitudeY = translationAmplitudeY;
        this.scaleAmplitude = scaleAmplitude;
        this.frequencyHz = Math.max(0f, frequencyHz);
    }

    @Override
    public void apply(AnimationPose.MutablePose pose, AnimatorContext context, float deltaTicks) {
        this.timeSeconds += Math.max(0f, deltaTicks) / 20f;

        float pulse = (float)Math.sin(this.timeSeconds * TWO_PI * this.frequencyHz);
        float yOffset = pulse * this.translationAmplitudeY;
        float scaleOffset = pulse * this.scaleAmplitude;

        Transform current = pose.transform(this.boneName);
        Vec3f newTranslation = current.translation().add(new Vec3f(0f, yOffset, 0f));
        Vec3f newScale = current.scale().add(new Vec3f(scaleOffset, scaleOffset, scaleOffset));

        pose.setTransform(this.boneName, new Transform(newTranslation, current.rotation(), newScale));
    }
}
