package io.hoyatla.animatruc.core.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-bone weight mask for layer blending.
 */
public final class BoneMask {
    public static final BoneMask FULL_BODY = new Builder().defaultWeight(1f).build();
    public static final BoneMask EMPTY = new Builder().defaultWeight(0f).build();

    private final float defaultWeight;
    private final Map<String, Float> explicitWeights;
    private final Map<String, Float> prefixWeights;

    private BoneMask(float defaultWeight, Map<String, Float> explicitWeights, Map<String, Float> prefixWeights) {
        this.defaultWeight = clamp(defaultWeight);
        this.explicitWeights = Collections.unmodifiableMap(explicitWeights);
        this.prefixWeights = Collections.unmodifiableMap(prefixWeights);
    }

    public float weightFor(String boneName) {
        Float explicit = this.explicitWeights.get(boneName);

        if (explicit != null)
            return explicit;

        float best = this.defaultWeight;
        int bestPrefixLength = -1;

        for (Map.Entry<String, Float> entry : this.prefixWeights.entrySet()) {
            String prefix = entry.getKey();

            if (boneName.startsWith(prefix) && prefix.length() > bestPrefixLength) {
                best = entry.getValue();
                bestPrefixLength = prefix.length();
            }
        }

        return best;
    }

    public static final class Builder {
        private float defaultWeight = 1f;
        private final Map<String, Float> explicitWeights = new HashMap<>();
        private final Map<String, Float> prefixWeights = new HashMap<>();

        public Builder defaultWeight(float value) {
            this.defaultWeight = clamp(value);
            return this;
        }

        public Builder set(String boneName, float weight) {
            Objects.requireNonNull(boneName, "boneName");
            this.explicitWeights.put(boneName, clamp(weight));
            return this;
        }

        public Builder include(String boneName) {
            return set(boneName, 1f);
        }

        public Builder exclude(String boneName) {
            return set(boneName, 0f);
        }

        public Builder setPrefix(String prefix, float weight) {
            Objects.requireNonNull(prefix, "prefix");
            this.prefixWeights.put(prefix, clamp(weight));
            return this;
        }

        public BoneMask build() {
            return new BoneMask(this.defaultWeight, new HashMap<>(this.explicitWeights), new HashMap<>(this.prefixWeights));
        }
    }

    private static float clamp(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
