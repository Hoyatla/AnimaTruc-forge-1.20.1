package io.hoyatla.animatruc.core.runtime;

public final class AnimaTrucRuntime {
    private static final AdaptiveUpdatePolicy DEFAULT_POLICY = AdaptiveUpdatePolicy.DEFAULT;

    private AnimaTrucRuntime() {}

    public static AnimatorInstance createAnimator() {
        return new AnimatorInstance(DEFAULT_POLICY);
    }

    public static AnimatorInstance createAnimator(AdaptiveUpdatePolicy policy) {
        return new AnimatorInstance(policy);
    }
}
