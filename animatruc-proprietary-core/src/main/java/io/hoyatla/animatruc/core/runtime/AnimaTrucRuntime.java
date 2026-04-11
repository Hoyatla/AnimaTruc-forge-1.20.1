package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.graph.AnimationGraph;
import io.hoyatla.animatruc.core.graph.AnimationGraphRuntime;

public final class AnimaTrucRuntime {
    private static final AdaptiveUpdatePolicy DEFAULT_POLICY = AdaptiveUpdatePolicy.DEFAULT;

    private AnimaTrucRuntime() {}

    public static AnimatorInstance createAnimator() {
        return new AnimatorInstance(DEFAULT_POLICY);
    }

    public static AnimatorInstance createAnimator(AdaptiveUpdatePolicy policy) {
        return new AnimatorInstance(policy);
    }

    public static AnimationGraphRuntime createGraphRuntime(AnimationGraph graph) {
        return new AnimationGraphRuntime(graph);
    }
}
