package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

/**
 * Procedural post-process modifier applied after clip blending.
 */
public interface AnimationModifier {
    void apply(AnimationPose.MutablePose pose, AnimatorContext context, float deltaTicks);
}
