package io.hoyatla.animatruc.core.math;

import java.util.Objects;

/**
 * Unit quaternion for bone rotation interpolation.
 */
public final class Quatf {
    public static final Quatf IDENTITY = new Quatf(0f, 0f, 0f, 1f);
    private static final float DEG_TO_RAD = (float)Math.PI / 180f;

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    public Quatf(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
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

    public float w() {
        return this.w;
    }

    public Quatf normalize() {
        float len = (float)Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);

        if (len == 0f)
            return IDENTITY;

        float inv = 1f / len;

        return new Quatf(this.x * inv, this.y * inv, this.z * inv, this.w * inv);
    }

    public Quatf multiply(Quatf other) {
        return new Quatf(
                this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y,
                this.w * other.y - this.x * other.z + this.y * other.w + this.z * other.x,
                this.w * other.z + this.x * other.y - this.y * other.x + this.z * other.w,
                this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z
        );
    }

    public static Quatf fromAxisAngle(float axisX, float axisY, float axisZ, float angleDegrees) {
        float radians = angleDegrees * DEG_TO_RAD;
        float half = radians * 0.5f;
        float sin = (float)Math.sin(half);
        float cos = (float)Math.cos(half);

        return new Quatf(axisX * sin, axisY * sin, axisZ * sin, cos).normalize();
    }

    public static Quatf fromEulerDegrees(float pitch, float yaw, float roll) {
        Quatf qx = fromAxisAngle(1f, 0f, 0f, pitch);
        Quatf qy = fromAxisAngle(0f, 1f, 0f, yaw);
        Quatf qz = fromAxisAngle(0f, 0f, 1f, roll);

        return qy.multiply(qx).multiply(qz).normalize();
    }

    public static Quatf nlerp(Quatf a, Quatf b, float alpha) {
        float t = clamp01(alpha);
        float dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;

        // Keep shortest path.
        Quatf target = dot < 0f ? new Quatf(-b.x, -b.y, -b.z, -b.w) : b;

        Quatf blended = new Quatf(
                a.x + (target.x - a.x) * t,
                a.y + (target.y - a.y) * t,
                a.z + (target.z - a.z) * t,
                a.w + (target.w - a.w) * t
        );

        return blended.normalize();
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
        return "Quatf{x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", w=" + this.w + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Quatf other))
            return false;

        return Float.compare(this.x, other.x) == 0
                && Float.compare(this.y, other.y) == 0
                && Float.compare(this.z, other.z) == 0
                && Float.compare(this.w, other.w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z, this.w);
    }
}
