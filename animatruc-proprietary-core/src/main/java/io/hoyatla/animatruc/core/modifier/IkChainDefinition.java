package io.hoyatla.animatruc.core.modifier;

import java.util.Objects;

/**
 * Immutable configuration of one two-bone IK chain.
 */
public final class IkChainDefinition {
    private final String id;
    private final String rootBone;
    private final String midBone;
    private final String endBone;
    private final String targetKey;
    private final IkPlane plane;
    private final float upperLength;
    private final float lowerLength;
    private final float smoothing;
    private final float bendDirection;
    private final String weightScalarKey;

    private IkChainDefinition(Builder builder) {
        this.id = require(builder.id, "id");
        this.rootBone = require(builder.rootBone, "rootBone");
        this.midBone = require(builder.midBone, "midBone");
        this.endBone = require(builder.endBone, "endBone");
        this.targetKey = require(builder.targetKey, "targetKey");
        this.plane = Objects.requireNonNull(builder.plane, "plane");
        this.upperLength = Math.max(0.001f, builder.upperLength);
        this.lowerLength = Math.max(0.001f, builder.lowerLength);
        this.smoothing = Math.max(0f, builder.smoothing);
        this.bendDirection = builder.bendDirection >= 0f ? 1f : -1f;
        this.weightScalarKey = builder.weightScalarKey;
    }

    public String id() {
        return this.id;
    }

    public String rootBone() {
        return this.rootBone;
    }

    public String midBone() {
        return this.midBone;
    }

    public String endBone() {
        return this.endBone;
    }

    public String targetKey() {
        return this.targetKey;
    }

    public IkPlane plane() {
        return this.plane;
    }

    public float upperLength() {
        return this.upperLength;
    }

    public float lowerLength() {
        return this.lowerLength;
    }

    public float smoothing() {
        return this.smoothing;
    }

    public float bendDirection() {
        return this.bendDirection;
    }

    public String weightScalarKey() {
        return this.weightScalarKey;
    }

    public static Builder builder(
            String id,
            String rootBone,
            String midBone,
            String endBone,
            String targetKey) {
        return new Builder(id, rootBone, midBone, endBone, targetKey);
    }

    public static final class Builder {
        private final String id;
        private final String rootBone;
        private final String midBone;
        private final String endBone;
        private final String targetKey;
        private IkPlane plane = IkPlane.YZ;
        private float upperLength = 1f;
        private float lowerLength = 1f;
        private float smoothing = 1f;
        private float bendDirection = 1f;
        private String weightScalarKey;

        private Builder(String id, String rootBone, String midBone, String endBone, String targetKey) {
            this.id = id;
            this.rootBone = rootBone;
            this.midBone = midBone;
            this.endBone = endBone;
            this.targetKey = targetKey;
        }

        public Builder plane(IkPlane value) {
            this.plane = Objects.requireNonNull(value, "value");
            return this;
        }

        public Builder lengths(float upper, float lower) {
            this.upperLength = upper;
            this.lowerLength = lower;
            return this;
        }

        public Builder smoothing(float value) {
            this.smoothing = value;
            return this;
        }

        public Builder bendDirection(float value) {
            this.bendDirection = value;
            return this;
        }

        public Builder weightScalarKey(String value) {
            this.weightScalarKey = value;
            return this;
        }

        public IkChainDefinition build() {
            return new IkChainDefinition(this);
        }
    }

    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be blank");

        return value;
    }
}
