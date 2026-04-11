package io.hoyatla.animatruc.core.importer;

/**
 * Import-time normalization options for authoring formats.
 */
public final class ModelImportOptions {
    public static final ModelImportOptions DEFAULT = builder().build();

    private final float translationScale;
    private final float ticksPerSecond;
    private final boolean defaultLooping;

    private ModelImportOptions(float translationScale, float ticksPerSecond, boolean defaultLooping) {
        this.translationScale = translationScale;
        this.ticksPerSecond = ticksPerSecond;
        this.defaultLooping = defaultLooping;
    }

    public float translationScale() {
        return this.translationScale;
    }

    public float ticksPerSecond() {
        return this.ticksPerSecond;
    }

    public boolean defaultLooping() {
        return this.defaultLooping;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float translationScale = 1f;
        private float ticksPerSecond = 20f;
        private boolean defaultLooping = true;

        private Builder() {
        }

        public Builder translationScale(float translationScale) {
            this.translationScale = translationScale;
            return this;
        }

        public Builder ticksPerSecond(float ticksPerSecond) {
            this.ticksPerSecond = ticksPerSecond;
            return this;
        }

        public Builder defaultLooping(boolean defaultLooping) {
            this.defaultLooping = defaultLooping;
            return this;
        }

        public ModelImportOptions build() {
            float safeScale = this.translationScale;
            float safeTicksPerSecond = this.ticksPerSecond <= 0f ? 20f : this.ticksPerSecond;

            return new ModelImportOptions(safeScale, safeTicksPerSecond, this.defaultLooping);
        }
    }
}
