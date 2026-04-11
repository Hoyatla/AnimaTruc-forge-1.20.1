package io.hoyatla.animatruc.core.animation;

@FunctionalInterface
public interface Interpolator<T> {
    T interpolate(T a, T b, float alpha);
}
