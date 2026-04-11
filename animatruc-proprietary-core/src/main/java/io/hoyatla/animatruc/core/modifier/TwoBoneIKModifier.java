package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

/**
 * 2-bone planar IK solver (XY plane), useful for arms/legs in procedural overlays.
 */
public final class TwoBoneIKModifier implements AnimationModifier {
    private final String rootBone;
    private final String midBone;
    private final String endBone;
    private final float upperLength;
    private final float lowerLength;
    private final float smoothing;

    public TwoBoneIKModifier(
            String rootBone,
            String midBone,
            String endBone,
            float upperLength,
            float lowerLength,
            float smoothing) {
        this.rootBone = rootBone;
        this.midBone = midBone;
        this.endBone = endBone;
        this.upperLength = Math.max(0.001f, upperLength);
        this.lowerLength = Math.max(0.001f, lowerLength);
        this.smoothing = Math.max(0f, smoothing);
    }

    @Override
    public void apply(AnimationPose.MutablePose pose, AnimatorContext context, float deltaTicks) {
        Vec3f target = context.ikTarget();

        if (target == null)
            return;

        float maxReach = this.upperLength + this.lowerLength - 0.0001f;
        Vec3f clampedTarget = target.clampLength(maxReach);
        float distance = Math.max(0.0001f, clampedTarget.length());

        float cosShoulder = clamp(
                (distance * distance + this.upperLength * this.upperLength - this.lowerLength * this.lowerLength)
                        / (2f * distance * this.upperLength),
                -1f,
                1f
        );
        float shoulderOffset = (float)Math.acos(cosShoulder);
        float baseAngle = (float)Math.atan2(clampedTarget.y(), clampedTarget.x());
        float rootAngle = baseAngle - shoulderOffset;

        float cosElbow = clamp(
                (this.upperLength * this.upperLength + this.lowerLength * this.lowerLength - distance * distance)
                        / (2f * this.upperLength * this.lowerLength),
                -1f,
                1f
        );
        float elbowAngle = (float)Math.PI - (float)Math.acos(cosElbow);

        float alpha = smoothingAlpha(deltaTicks);

        Transform root = pose.transform(this.rootBone);
        Transform mid = pose.transform(this.midBone);
        Transform end = pose.transform(this.endBone);

        Quatf rootTarget = Quatf.fromEulerDegrees(0f, 0f, radiansToDegrees(rootAngle));
        Quatf midTarget = Quatf.fromEulerDegrees(0f, 0f, radiansToDegrees(elbowAngle));

        pose.setTransform(this.rootBone, root.withRotation(Quatf.nlerp(root.rotation(), rootTarget, alpha)));
        pose.setTransform(this.midBone, mid.withRotation(Quatf.nlerp(mid.rotation(), midTarget, alpha)));
        pose.setTransform(this.endBone, end);
    }

    private float smoothingAlpha(float deltaTicks) {
        if (this.smoothing == 0f)
            return 1f;

        return Math.min(1f, this.smoothing * Math.max(0f, deltaTicks));
    }

    private static float radiansToDegrees(float radians) {
        return (float)(radians * 180d / Math.PI);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
