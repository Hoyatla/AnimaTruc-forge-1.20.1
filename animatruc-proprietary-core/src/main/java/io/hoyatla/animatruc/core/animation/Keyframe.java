package io.hoyatla.animatruc.core.animation;

import java.util.Objects;

public final class Keyframe<T> {
    private final float tick;
    private final T value;
    private final InterpolationMode interpolationMode;

    public Keyframe(float tick, T value, InterpolationMode interpolationMode) {
        this.tick = tick;
        this.value = Objects.requireNonNull(value, "value");
        this.interpolationMode = Objects.requireNonNull(interpolationMode, "interpolationMode");
    }

    public float tick() {
        return this.tick;
    }

    public T value() {
        return this.value;
    }

    public InterpolationMode interpolationMode() {
        return this.interpolationMode;
    }
}
