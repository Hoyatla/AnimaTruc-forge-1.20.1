package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Multi-chain two-bone IK modifier for procedural foot/leg overlays.
 * Target vectors are resolved from {@link AnimatorContext#vector(String, Vec3f)}.
 */
public final class MultiLimbIKModifier implements AnimationModifier {
    private final List<IkChainDefinition> chains;
    private final boolean legacyIkFallback;

    public MultiLimbIKModifier(List<IkChainDefinition> chains, boolean legacyIkFallback) {
        if (chains == null || chains.isEmpty())
            throw new IllegalArgumentException("chains must not be empty");

        this.chains = List.copyOf(chains);
        this.legacyIkFallback = legacyIkFallback;
    }

    public static MultiLimbIKModifier of(IkChainDefinition... chains) {
        List<IkChainDefinition> list = new ArrayList<>(Objects.requireNonNull(chains, "chains").length);

        for (IkChainDefinition chain : chains) {
            if (chain != null)
                list.add(chain);
        }

        return new MultiLimbIKModifier(list, true);
    }

    @Override
    public void apply(AnimationPose.MutablePose pose, AnimatorContext context, float deltaTicks) {
        float safeDelta = Math.max(0f, deltaTicks);

        for (IkChainDefinition chain : this.chains) {
            Vec3f target = context.vector(chain.targetKey(), null);

            if (target == null && this.legacyIkFallback)
                target = context.ikTarget();

            if (target == null)
                continue;

            float alpha = smoothingAlpha(chain.smoothing(), safeDelta);

            if (chain.weightScalarKey() != null)
                alpha *= clamp01(context.scalar(chain.weightScalarKey(), 1f));

            if (alpha <= 0f)
                continue;

            solveChain(pose, chain, target, alpha);
        }
    }

    private static void solveChain(AnimationPose.MutablePose pose, IkChainDefinition chain, Vec3f target, float alpha) {
        float componentA = selectA(target, chain.plane());
        float componentB = selectB(target, chain.plane());

        float maxReach = chain.upperLength() + chain.lowerLength() - 0.0001f;
        float distance = (float)Math.sqrt(componentA * componentA + componentB * componentB);
        float clampedDistance = Math.min(Math.max(distance, 0.0001f), maxReach);
        float directionScale = distance <= 0.0001f ? 1f : clampedDistance / distance;
        float targetA = componentA * directionScale;
        float targetB = componentB * directionScale;

        float cosShoulder = clamp(
                (clampedDistance * clampedDistance + chain.upperLength() * chain.upperLength()
                        - chain.lowerLength() * chain.lowerLength())
                        / (2f * clampedDistance * chain.upperLength()),
                -1f,
                1f
        );
        float shoulderOffset = (float)Math.acos(cosShoulder);
        float baseAngle = (float)Math.atan2(targetB, targetA);
        float rootAngle = baseAngle - shoulderOffset * chain.bendDirection();

        float cosElbow = clamp(
                (chain.upperLength() * chain.upperLength() + chain.lowerLength() * chain.lowerLength()
                        - clampedDistance * clampedDistance)
                        / (2f * chain.upperLength() * chain.lowerLength()),
                -1f,
                1f
        );
        float elbowAngle = ((float)Math.PI - (float)Math.acos(cosElbow)) * chain.bendDirection();

        Transform root = pose.transform(chain.rootBone());
        Transform mid = pose.transform(chain.midBone());
        Transform end = pose.transform(chain.endBone());

        Quatf rootTarget = fromPlaneAngle(chain.plane(), radiansToDegrees(rootAngle));
        Quatf midTarget = fromPlaneAngle(chain.plane(), radiansToDegrees(elbowAngle));

        pose.setTransform(chain.rootBone(), root.withRotation(Quatf.nlerp(root.rotation(), rootTarget, alpha)));
        pose.setTransform(chain.midBone(), mid.withRotation(Quatf.nlerp(mid.rotation(), midTarget, alpha)));
        pose.setTransform(chain.endBone(), end);
    }

    private static float selectA(Vec3f vector, IkPlane plane) {
        return switch (plane) {
            case XY -> vector.x();
            case YZ -> vector.z();
            case XZ -> vector.x();
        };
    }

    private static float selectB(Vec3f vector, IkPlane plane) {
        return switch (plane) {
            case XY -> vector.y();
            case YZ -> vector.y();
            case XZ -> vector.z();
        };
    }

    private static Quatf fromPlaneAngle(IkPlane plane, float angleDegrees) {
        return switch (plane) {
            case XY -> Quatf.fromEulerDegrees(0f, 0f, angleDegrees);
            case YZ -> Quatf.fromEulerDegrees(angleDegrees, 0f, 0f);
            case XZ -> Quatf.fromEulerDegrees(0f, angleDegrees, 0f);
        };
    }

    private static float smoothingAlpha(float smoothing, float deltaTicks) {
        if (smoothing <= 0f)
            return 1f;

        return Math.min(1f, smoothing * deltaTicks);
    }

    private static float radiansToDegrees(float radians) {
        return (float)(radians * 180d / Math.PI);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }
}
