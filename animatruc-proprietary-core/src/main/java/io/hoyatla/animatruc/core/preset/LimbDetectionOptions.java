package io.hoyatla.animatruc.core.preset;

/**
 * Tunable options for automatic leg-chain detection.
 */
public final class LimbDetectionOptions {
    public static final LimbDetectionOptions DEFAULT = builder().build();

    private final float minConfidence;
    private final int maxDetectedChains;
    private final int maxMyriapodChains;
    private final boolean allowBiped;
    private final boolean allowHexapod;
    private final boolean allowOctopod;
    private final boolean allowMyriapod;

    private LimbDetectionOptions(Builder builder) {
        this.minConfidence = clamp01(builder.minConfidence);
        this.maxDetectedChains = Math.max(1, builder.maxDetectedChains);
        this.maxMyriapodChains = Math.max(8, builder.maxMyriapodChains);
        this.allowBiped = builder.allowBiped;
        this.allowHexapod = builder.allowHexapod;
        this.allowOctopod = builder.allowOctopod;
        this.allowMyriapod = builder.allowMyriapod;
    }

    public float minConfidence() {
        return this.minConfidence;
    }

    public int maxDetectedChains() {
        return this.maxDetectedChains;
    }

    public int maxMyriapodChains() {
        return this.maxMyriapodChains;
    }

    public boolean allowBiped() {
        return this.allowBiped;
    }

    public boolean allowHexapod() {
        return this.allowHexapod;
    }

    public boolean allowOctopod() {
        return this.allowOctopod;
    }

    public boolean allowMyriapod() {
        return this.allowMyriapod;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float minConfidence = 0.55f;
        private int maxDetectedChains = 16;
        private int maxMyriapodChains = 24;
        private boolean allowBiped = true;
        private boolean allowHexapod = true;
        private boolean allowOctopod = true;
        private boolean allowMyriapod = true;

        public Builder minConfidence(float value) {
            this.minConfidence = value;
            return this;
        }

        public Builder maxDetectedChains(int value) {
            this.maxDetectedChains = value;
            return this;
        }

        public Builder maxMyriapodChains(int value) {
            this.maxMyriapodChains = value;
            return this;
        }

        public Builder allowBiped(boolean value) {
            this.allowBiped = value;
            return this;
        }

        public Builder allowHexapod(boolean value) {
            this.allowHexapod = value;
            return this;
        }

        public Builder allowOctopod(boolean value) {
            this.allowOctopod = value;
            return this;
        }

        public Builder allowMyriapod(boolean value) {
            this.allowMyriapod = value;
            return this;
        }

        public LimbDetectionOptions build() {
            return new LimbDetectionOptions(this);
        }
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
