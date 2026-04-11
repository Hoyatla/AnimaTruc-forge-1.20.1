package io.hoyatla.animatruc.core.animation;

import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

/**
 * Mutable internal accumulator used by the mixer hot path.
 */
final class WeightedTransformAccumulator {
    private Vec3f translation = Vec3f.ZERO;
    private Vec3f scale = Vec3f.ONE;
    private Quatf rotation = Quatf.IDENTITY;
    private float weightSum = 0f;
    private boolean rotationInitialized = false;

    static WeightedTransformAccumulator base() {
        return new WeightedTransformAccumulator();
    }

    void addWeighted(Transform transform, float weight) {
        float clamped = Math.max(0f, weight);

        if (clamped <= 0f)
            return;

        this.translation = this.translation.add(transform.translation().multiply(clamped));
        this.scale = this.scale.add(transform.scale().subtract(Vec3f.ONE).multiply(clamped));

        if (!this.rotationInitialized) {
            this.rotation = transform.rotation();
            this.rotationInitialized = true;
        }
        else {
            float blendAlpha = clamped / (this.weightSum + clamped);
            this.rotation = Quatf.nlerp(this.rotation, transform.rotation(), blendAlpha);
        }

        this.weightSum += clamped;
    }

    void addAdditive(Transform transform, float weight) {
        float clamped = Math.max(0f, weight);

        if (clamped <= 0f)
            return;

        this.translation = this.translation.add(transform.translation().multiply(clamped));
        this.scale = this.scale.add(transform.scale().subtract(Vec3f.ONE).multiply(clamped));
        this.rotation = Quatf.nlerp(this.rotation, this.rotation.multiply(transform.rotation()), clamped);
    }

    Transform resolve() {
        if (this.weightSum > 0f) {
            float inv = 1f / this.weightSum;

            return new Transform(
                    this.translation.multiply(inv),
                    this.rotation.normalize(),
                    this.scale
            );
        }

        return new Transform(this.translation, this.rotation.normalize(), this.scale);
    }
}
