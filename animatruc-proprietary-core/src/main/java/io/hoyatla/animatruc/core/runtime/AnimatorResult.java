package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.animation.AnimationPose;

public record AnimatorResult(AnimationPose pose, boolean advancedThisTick) {
}
