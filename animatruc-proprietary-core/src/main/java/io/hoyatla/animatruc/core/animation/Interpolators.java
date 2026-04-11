package io.hoyatla.animatruc.core.animation;

import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

public final class Interpolators {
    public static final Interpolator<Float> FLOAT = (a, b, alpha) -> a + (b - a) * clamp01(alpha);
    public static final Interpolator<Vec3f> VEC3 = Vec3f::lerp;
    public static final Interpolator<Quatf> QUAT = Quatf::nlerp;

    private Interpolators() {}

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
