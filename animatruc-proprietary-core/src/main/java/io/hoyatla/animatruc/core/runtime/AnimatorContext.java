package io.hoyatla.animatruc.core.runtime;

/**
 * Runtime update context, typically supplied by the renderer or simulation layer.
 */
public record AnimatorContext(float distanceToCamera, boolean visible, boolean forceTick) {
    public static AnimatorContext visibleNear() {
        return new AnimatorContext(0f, true, false);
    }
}
