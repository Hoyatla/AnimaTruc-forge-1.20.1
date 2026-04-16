package io.hoyatla.animatruc.forge.ground;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Runtime limb contact configuration used by {@link GroundContactController}.
 */
public final class GroundContactLimbDefinition {
    private final String id;
    private final String targetKey;
    private final Vec3f anchorLocal;
    private final float stepDistance;
    private final float stepTriggerDistance;
    private final float liftHeight;
    private final float probeUpDistance;
    private final float probeDownDistance;
    private final float phaseOffset;

    private GroundContactLimbDefinition(Builder builder) {
        this.id = require(builder.id, "id");
        this.targetKey = require(builder.targetKey, "targetKey");
        this.anchorLocal = Objects.requireNonNull(builder.anchorLocal, "anchorLocal");
        this.stepDistance = Math.max(0f, builder.stepDistance);
        this.stepTriggerDistance = Math.max(0.001f, builder.stepTriggerDistance);
        this.liftHeight = Math.max(0f, builder.liftHeight);
        this.probeUpDistance = Math.max(0f, builder.probeUpDistance);
        this.probeDownDistance = Math.max(0.01f, builder.probeDownDistance);
        this.phaseOffset = builder.phaseOffset;
    }

    public String id() {
        return this.id;
    }

    public String targetKey() {
        return this.targetKey;
    }

    public Vec3f anchorLocal() {
        return this.anchorLocal;
    }

    public float stepDistance() {
        return this.stepDistance;
    }

    public float stepTriggerDistance() {
        return this.stepTriggerDistance;
    }

    public float liftHeight() {
        return this.liftHeight;
    }

    public float probeUpDistance() {
        return this.probeUpDistance;
    }

    public float probeDownDistance() {
        return this.probeDownDistance;
    }

    public float phaseOffset() {
        return this.phaseOffset;
    }

    public static Builder builder(String id, String targetKey, Vec3f anchorLocal) {
        return new Builder(id, targetKey, anchorLocal);
    }

    public static final class Builder {
        private final String id;
        private final String targetKey;
        private final Vec3f anchorLocal;
        private float stepDistance = 0.3f;
        private float stepTriggerDistance = 0.2f;
        private float liftHeight = 0.15f;
        private float probeUpDistance = 0.5f;
        private float probeDownDistance = 1.8f;
        private float phaseOffset;

        private Builder(String id, String targetKey, Vec3f anchorLocal) {
            this.id = id;
            this.targetKey = targetKey;
            this.anchorLocal = anchorLocal;
        }

        public Builder stepDistance(float value) {
            this.stepDistance = value;
            return this;
        }

        public Builder stepTriggerDistance(float value) {
            this.stepTriggerDistance = value;
            return this;
        }

        public Builder liftHeight(float value) {
            this.liftHeight = value;
            return this;
        }

        public Builder probe(float upDistance, float downDistance) {
            this.probeUpDistance = upDistance;
            this.probeDownDistance = downDistance;
            return this;
        }

        public Builder phaseOffset(float value) {
            this.phaseOffset = value;
            return this;
        }

        public GroundContactLimbDefinition build() {
            return new GroundContactLimbDefinition(this);
        }
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be blank");

        return value;
    }
}
