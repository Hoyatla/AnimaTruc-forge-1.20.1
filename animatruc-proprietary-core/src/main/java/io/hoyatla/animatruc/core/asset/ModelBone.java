package io.hoyatla.animatruc.core.asset;

import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Immutable skeleton bone definition imported from authoring assets.
 */
public final class ModelBone {
    private final String name;
    private final String parentName;
    private final Vec3f pivot;
    private final Transform bindTransform;

    public ModelBone(String name, String parentName, Vec3f pivot, Transform bindTransform) {
        this.name = Objects.requireNonNull(name, "name");
        this.parentName = parentName;
        this.pivot = Objects.requireNonNull(pivot, "pivot");
        this.bindTransform = Objects.requireNonNull(bindTransform, "bindTransform");
    }

    public String name() {
        return this.name;
    }

    public String parentName() {
        return this.parentName;
    }

    public Vec3f pivot() {
        return this.pivot;
    }

    public Transform bindTransform() {
        return this.bindTransform;
    }
}
