package io.hoyatla.animatruc.core.animation;

import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Local bone transform.
 */
public final class Transform {
    public static final Transform IDENTITY = new Transform(Vec3f.ZERO, Quatf.IDENTITY, Vec3f.ONE);

    private final Vec3f translation;
    private final Quatf rotation;
    private final Vec3f scale;

    public Transform(Vec3f translation, Quatf rotation, Vec3f scale) {
        this.translation = Objects.requireNonNull(translation, "translation");
        this.rotation = Objects.requireNonNull(rotation, "rotation");
        this.scale = Objects.requireNonNull(scale, "scale");
    }

    public Vec3f translation() {
        return this.translation;
    }

    public Quatf rotation() {
        return this.rotation;
    }

    public Vec3f scale() {
        return this.scale;
    }

    public static Transform blend(Transform a, Transform b, float alpha) {
        return new Transform(
                Vec3f.lerp(a.translation, b.translation, alpha),
                Quatf.nlerp(a.rotation, b.rotation, alpha),
                Vec3f.lerp(a.scale, b.scale, alpha)
        );
    }

    public Transform addWeighted(Transform delta, float weight) {
        return new Transform(
                this.translation.add(delta.translation.multiply(weight)),
                Quatf.nlerp(this.rotation, this.rotation.multiply(delta.rotation), weight),
                this.scale.add(delta.scale.subtract(Vec3f.ONE).multiply(weight))
        );
    }
}
