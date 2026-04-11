package io.hoyatla.animatruc.core.math;

import java.util.Objects;

/**
 * Immutable 3D vector with allocation-free helpers for runtime animation math.
 */
public final class Vec3f {
    public static final Vec3f ZERO = new Vec3f(0f, 0f, 0f);
    public static final Vec3f ONE = new Vec3f(1f, 1f, 1f);

    private final float x;
    private final float y;
    private final float z;

    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public Vec3f add(Vec3f other) {
        return new Vec3f(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public Vec3f subtract(Vec3f other) {
        return new Vec3f(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vec3f multiply(float scalar) {
        return new Vec3f(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vec3f multiply(Vec3f other) {
        return new Vec3f(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public float dot(Vec3f other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public float lengthSquared() {
        return this.dot(this);
    }

    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }

    public Vec3f normalize() {
        float len = length();

        if (len == 0f)
            return ZERO;

        return multiply(1f / len);
    }

    public Vec3f clampLength(float maxLength) {
        if (maxLength <= 0f)
            return ZERO;

        float lengthSquared = lengthSquared();
        float maxLengthSquared = maxLength * maxLength;

        if (lengthSquared <= maxLengthSquared)
            return this;

        return normalize().multiply(maxLength);
    }

    public static Vec3f lerp(Vec3f a, Vec3f b, float alpha) {
        float t = clamp01(alpha);

        return new Vec3f(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }

    @Override
    public String toString() {
        return "Vec3f{x=" + this.x + ", y=" + this.y + ", z=" + this.z + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Vec3f other))
            return false;

        return Float.compare(this.x, other.x) == 0
                && Float.compare(this.y, other.y) == 0
                && Float.compare(this.z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}
