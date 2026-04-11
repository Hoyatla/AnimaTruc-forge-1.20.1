package io.hoyatla.animatruc.core.asset;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Cube geometry bound to a skeleton bone.
 */
public final class ModelCube {
    private final String name;
    private final String boneName;
    private final Vec3f from;
    private final Vec3f to;
    private final float inflate;
    private final boolean mirror;

    public ModelCube(String name, String boneName, Vec3f from, Vec3f to, float inflate, boolean mirror) {
        this.name = Objects.requireNonNull(name, "name");
        this.boneName = Objects.requireNonNull(boneName, "boneName");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.inflate = inflate;
        this.mirror = mirror;
    }

    public String name() {
        return this.name;
    }

    public String boneName() {
        return this.boneName;
    }

    public Vec3f from() {
        return this.from;
    }

    public Vec3f to() {
        return this.to;
    }

    public float inflate() {
        return this.inflate;
    }

    public boolean mirror() {
        return this.mirror;
    }
}
