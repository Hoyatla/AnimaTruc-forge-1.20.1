package io.hoyatla.animatruc.core.graph;

import io.hoyatla.animatruc.core.runtime.AnimatorContext;

@FunctionalInterface
public interface AnimationGraphCondition {
    AnimationGraphCondition ALWAYS = (parameters, context) -> true;

    boolean test(GraphParameters parameters, AnimatorContext context);
}
