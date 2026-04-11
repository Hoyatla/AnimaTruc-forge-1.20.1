package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Map;

/**
 * Runtime update context, typically supplied by the renderer or simulation layer.
 */
public record AnimatorContext(
        float distanceToCamera,
        boolean visible,
        boolean forceTick,
        float lookYawDegrees,
        float lookPitchDegrees,
        Vec3f ikTarget,
        Map<String, Float> scalarParameters) {
    public AnimatorContext(float distanceToCamera, boolean visible, boolean forceTick) {
        this(distanceToCamera, visible, forceTick, 0f, 0f, null, Map.of());
    }

    public AnimatorContext {
        scalarParameters = scalarParameters == null ? Map.of() : Map.copyOf(scalarParameters);
    }

    public float scalar(String key, float fallback) {
        Float value = this.scalarParameters.get(key);

        return value == null ? fallback : value;
    }

    public Builder toBuilder() {
        return new Builder()
                .distanceToCamera(this.distanceToCamera)
                .visible(this.visible)
                .forceTick(this.forceTick)
                .lookYawDegrees(this.lookYawDegrees)
                .lookPitchDegrees(this.lookPitchDegrees)
                .ikTarget(this.ikTarget)
                .scalarParameters(this.scalarParameters);
    }

    public static AnimatorContext visibleNear() {
        return new AnimatorContext(0f, true, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float distanceToCamera;
        private boolean visible = true;
        private boolean forceTick;
        private float lookYawDegrees;
        private float lookPitchDegrees;
        private Vec3f ikTarget;
        private Map<String, Float> scalarParameters = Map.of();

        public Builder distanceToCamera(float value) {
            this.distanceToCamera = value;
            return this;
        }

        public Builder visible(boolean value) {
            this.visible = value;
            return this;
        }

        public Builder forceTick(boolean value) {
            this.forceTick = value;
            return this;
        }

        public Builder lookYawDegrees(float value) {
            this.lookYawDegrees = value;
            return this;
        }

        public Builder lookPitchDegrees(float value) {
            this.lookPitchDegrees = value;
            return this;
        }

        public Builder ikTarget(Vec3f value) {
            this.ikTarget = value;
            return this;
        }

        public Builder scalarParameters(Map<String, Float> values) {
            this.scalarParameters = values == null ? Map.of() : Map.copyOf(values);
            return this;
        }

        public AnimatorContext build() {
            return new AnimatorContext(
                    this.distanceToCamera,
                    this.visible,
                    this.forceTick,
                    this.lookYawDegrees,
                    this.lookPitchDegrees,
                    this.ikTarget,
                    this.scalarParameters
            );
        }
    }
}
