package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

/**
 * Simple look-at correction using runtime yaw/pitch inputs.
 */
public final class LookAtModifier implements AnimationModifier {
    private final String boneName;
    private final float yawScale;
    private final float pitchScale;
    private final float maxYaw;
    private final float maxPitch;
    private final float smoothing;

    private float smoothedYaw;
    private float smoothedPitch;

    public LookAtModifier(String boneName, float yawScale, float pitchScale, float maxYaw, float maxPitch, float smoothing) {
        this.boneName = boneName;
        this.yawScale = yawScale;
        this.pitchScale = pitchScale;
        this.maxYaw = Math.max(0f, maxYaw);
        this.maxPitch = Math.max(0f, maxPitch);
        this.smoothing = Math.max(0f, smoothing);
    }

    @Override
    public void apply(AnimationPose.MutablePose pose, AnimatorContext context, float deltaTicks) {
        float desiredYaw = clamp(context.lookYawDegrees() * this.yawScale, -this.maxYaw, this.maxYaw);
        float desiredPitch = clamp(context.lookPitchDegrees() * this.pitchScale, -this.maxPitch, this.maxPitch);
        float alpha = smoothingAlpha(deltaTicks);

        this.smoothedYaw += (desiredYaw - this.smoothedYaw) * alpha;
        this.smoothedPitch += (desiredPitch - this.smoothedPitch) * alpha;

        Transform current = pose.transform(this.boneName);
        Quatf lookRotation = Quatf.fromEulerDegrees(this.smoothedPitch, this.smoothedYaw, 0f);
        Quatf blended = Quatf.nlerp(current.rotation(), lookRotation, alpha);

        pose.setTransform(this.boneName, current.withRotation(blended));
    }

    private float smoothingAlpha(float deltaTicks) {
        if (this.smoothing == 0f)
            return 1f;

        float alpha = this.smoothing * Math.max(0f, deltaTicks);

        if (alpha >= 1f)
            return 1f;

        return alpha;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
