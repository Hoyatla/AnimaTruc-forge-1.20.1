package io.hoyatla.animatruc.core.preset;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Objects;

/**
 * Result of leg-chain auto detection from a skeleton hierarchy.
 */
public final class DetectedLimbChain {
    private final String id;
    private final String rootBone;
    private final String midBone;
    private final String endBone;
    private final String targetKey;
    private final LimbSide side;
    private final Vec3f anchorLocal;
    private final float upperLength;
    private final float lowerLength;
    private final float confidence;
    private final float phaseOffset;

    public DetectedLimbChain(
            String id,
            String rootBone,
            String midBone,
            String endBone,
            String targetKey,
            LimbSide side,
            Vec3f anchorLocal,
            float upperLength,
            float lowerLength,
            float confidence,
            float phaseOffset) {
        this.id = require(id, "id");
        this.rootBone = require(rootBone, "rootBone");
        this.midBone = require(midBone, "midBone");
        this.endBone = require(endBone, "endBone");
        this.targetKey = require(targetKey, "targetKey");
        this.side = Objects.requireNonNull(side, "side");
        this.anchorLocal = Objects.requireNonNull(anchorLocal, "anchorLocal");
        this.upperLength = Math.max(0.001f, upperLength);
        this.lowerLength = Math.max(0.001f, lowerLength);
        this.confidence = clamp01(confidence);
        this.phaseOffset = clamp01(phaseOffset);
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

    public LimbSide side() {
        return this.side;
    }

    public Vec3f anchorLocal() {
        return this.anchorLocal;
    }

    public float upperLength() {
        return this.upperLength;
    }

    public float lowerLength() {
        return this.lowerLength;
    }

    public float confidence() {
        return this.confidence;
    }

    public float phaseOffset() {
        return this.phaseOffset;
    }

    public DetectedLimbChain withPhaseOffset(float value) {
        return new DetectedLimbChain(
                this.id,
                this.rootBone,
                this.midBone,
                this.endBone,
                this.targetKey,
                this.side,
                this.anchorLocal,
                this.upperLength,
                this.lowerLength,
                this.confidence,
                value
        );
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(name + " must not be blank");

        return value;
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
