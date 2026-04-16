package io.hoyatla.animatruc.core.preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable output of automatic locomotion preset detection.
 */
public final class LimbDetectionReport {
    private final LocomotionPresetType presetType;
    private final List<DetectedLimbChain> chains;
    private final float averageConfidence;
    private final boolean accepted;
    private final List<String> warnings;

    public LimbDetectionReport(
            LocomotionPresetType presetType,
            List<DetectedLimbChain> chains,
            float averageConfidence,
            boolean accepted,
            List<String> warnings) {
        this.presetType = Objects.requireNonNull(presetType, "presetType");
        this.chains = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(chains, "chains")));
        this.averageConfidence = clamp01(averageConfidence);
        this.accepted = accepted;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(warnings, "warnings")));
    }

    public LocomotionPresetType presetType() {
        return this.presetType;
    }

    public List<DetectedLimbChain> chains() {
        return this.chains;
    }

    public float averageConfidence() {
        return this.averageConfidence;
    }

    public boolean accepted() {
        return this.accepted;
    }

    public List<String> warnings() {
        return this.warnings;
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
