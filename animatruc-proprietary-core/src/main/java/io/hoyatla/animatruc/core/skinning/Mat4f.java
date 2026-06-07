package io.hoyatla.animatruc.core.skinning;

import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

/**
 * Small affine 4x4 matrix utility for CPU skinning.
 */
public final class Mat4f {
    private static final float EPSILON = 1.0e-8f;

    public static final Mat4f IDENTITY = new Mat4f(new float[]{
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
    });

    private final float[] m;

    private Mat4f(float[] m) {
        this.m = m;
    }

    public static Mat4f trs(Vec3f translation, Quatf rotation, Vec3f scale) {
        float x = rotation.x();
        float y = rotation.y();
        float z = rotation.z();
        float w = rotation.w();

        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;

        float sx = scale.x();
        float sy = scale.y();
        float sz = scale.z();

        return new Mat4f(new float[]{
                (1f - 2f * (yy + zz)) * sx, (2f * (xy - wz)) * sy, (2f * (xz + wy)) * sz, translation.x(),
                (2f * (xy + wz)) * sx, (1f - 2f * (xx + zz)) * sy, (2f * (yz - wx)) * sz, translation.y(),
                (2f * (xz - wy)) * sx, (2f * (yz + wx)) * sy, (1f - 2f * (xx + yy)) * sz, translation.z(),
                0f, 0f, 0f, 1f
        });
    }

    public Mat4f multiply(Mat4f other) {
        float[] out = new float[16];

        for (int row = 0; row < 4; row++) {
            int rowOffset = row * 4;
            for (int col = 0; col < 4; col++) {
                out[rowOffset + col] =
                        this.m[rowOffset] * other.m[col]
                                + this.m[rowOffset + 1] * other.m[4 + col]
                                + this.m[rowOffset + 2] * other.m[8 + col]
                                + this.m[rowOffset + 3] * other.m[12 + col];
            }
        }

        return new Mat4f(out);
    }

    public Mat4f invertAffine() {
        float a00 = this.m[0];
        float a01 = this.m[1];
        float a02 = this.m[2];
        float a10 = this.m[4];
        float a11 = this.m[5];
        float a12 = this.m[6];
        float a20 = this.m[8];
        float a21 = this.m[9];
        float a22 = this.m[10];

        float det =
                a00 * (a11 * a22 - a12 * a21)
                        - a01 * (a10 * a22 - a12 * a20)
                        + a02 * (a10 * a21 - a11 * a20);

        if (Math.abs(det) <= EPSILON)
            return IDENTITY;

        float invDet = 1f / det;

        float i00 = (a11 * a22 - a12 * a21) * invDet;
        float i01 = (a02 * a21 - a01 * a22) * invDet;
        float i02 = (a01 * a12 - a02 * a11) * invDet;
        float i10 = (a12 * a20 - a10 * a22) * invDet;
        float i11 = (a00 * a22 - a02 * a20) * invDet;
        float i12 = (a02 * a10 - a00 * a12) * invDet;
        float i20 = (a10 * a21 - a11 * a20) * invDet;
        float i21 = (a01 * a20 - a00 * a21) * invDet;
        float i22 = (a00 * a11 - a01 * a10) * invDet;

        float tx = this.m[3];
        float ty = this.m[7];
        float tz = this.m[11];

        return new Mat4f(new float[]{
                i00, i01, i02, -(i00 * tx + i01 * ty + i02 * tz),
                i10, i11, i12, -(i10 * tx + i11 * ty + i12 * tz),
                i20, i21, i22, -(i20 * tx + i21 * ty + i22 * tz),
                0f, 0f, 0f, 1f
        });
    }

    public Vec3f transformPosition(Vec3f value) {
        return new Vec3f(
                this.m[0] * value.x() + this.m[1] * value.y() + this.m[2] * value.z() + this.m[3],
                this.m[4] * value.x() + this.m[5] * value.y() + this.m[6] * value.z() + this.m[7],
                this.m[8] * value.x() + this.m[9] * value.y() + this.m[10] * value.z() + this.m[11]
        );
    }

    public Vec3f transformDirection(Vec3f value) {
        return new Vec3f(
                this.m[0] * value.x() + this.m[1] * value.y() + this.m[2] * value.z(),
                this.m[4] * value.x() + this.m[5] * value.y() + this.m[6] * value.z(),
                this.m[8] * value.x() + this.m[9] * value.y() + this.m[10] * value.z()
        );
    }
}
