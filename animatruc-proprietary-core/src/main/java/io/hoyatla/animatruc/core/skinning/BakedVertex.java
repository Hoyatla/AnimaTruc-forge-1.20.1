package io.hoyatla.animatruc.core.skinning;

import io.hoyatla.animatruc.core.asset.ModelUv;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Final CPU-skinned vertex sample ready for rendering.
 */
public final class BakedVertex {
    private final Vec3f position;
    private final Vec3f normal;
    private final ModelUv uv;

    public BakedVertex(Vec3f position, Vec3f normal, ModelUv uv) {
        this.position = Objects.requireNonNull(position, "position");
        this.normal = Objects.requireNonNull(normal, "normal");
        this.uv = Objects.requireNonNull(uv, "uv");
    }

    public Vec3f position() {
        return this.position;
    }

    public Vec3f normal() {
        return this.normal;
    }

    public ModelUv uv() {
        return this.uv;
    }
}
