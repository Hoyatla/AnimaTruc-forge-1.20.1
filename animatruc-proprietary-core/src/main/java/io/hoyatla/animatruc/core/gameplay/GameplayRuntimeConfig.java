package io.hoyatla.animatruc.core.gameplay;

public record GameplayRuntimeConfig(
        boolean masterEnabled,
        boolean locomotionEnabled,
        boolean emotesEnabled,
        boolean perceptionEnabled,
        boolean combatFeedbackEnabled,
        boolean explosionFeedbackEnabled,
        boolean weightEnabled,
        boolean inputUiEnabled,
        float maxStamina,
        float staminaRecoveryPerTick,
        float staminaCostScale,
        float perceptionRange,
        float explosionFeedbackRange,
        float suppressionRecoveryPerTick,
        float weightOrangeRatio,
        float weightRedRatio) {
    public static final GameplayRuntimeConfig DEFAULT = builder().build();

    public GameplayRuntimeConfig {
        maxStamina = Math.max(1f, maxStamina);
        staminaRecoveryPerTick = Math.max(0f, staminaRecoveryPerTick);
        staminaCostScale = Math.max(0f, staminaCostScale);
        perceptionRange = Math.max(1f, perceptionRange);
        explosionFeedbackRange = Math.max(1f, explosionFeedbackRange);
        suppressionRecoveryPerTick = Math.max(0f, suppressionRecoveryPerTick);
        weightOrangeRatio = Math.max(0.01f, weightOrangeRatio);
        weightRedRatio = Math.max(weightOrangeRatio, weightRedRatio);
    }

    public boolean featureEnabled(GameplayFeature feature) {
        if (!this.masterEnabled)
            return false;

        return switch (feature) {
            case LOCOMOTION -> this.locomotionEnabled;
            case EMOTES -> this.emotesEnabled;
            case PERCEPTION -> this.perceptionEnabled;
            case COMBAT_FEEDBACK -> this.combatFeedbackEnabled;
            case EXPLOSION_FEEDBACK -> this.explosionFeedbackEnabled;
            case WEIGHT -> this.weightEnabled;
            case INPUT_UI -> this.inputUiEnabled;
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean masterEnabled = true;
        private boolean locomotionEnabled = true;
        private boolean emotesEnabled = true;
        private boolean perceptionEnabled = true;
        private boolean combatFeedbackEnabled = true;
        private boolean explosionFeedbackEnabled = true;
        private boolean weightEnabled = true;
        private boolean inputUiEnabled = true;
        private float maxStamina = 100f;
        private float staminaRecoveryPerTick = 0.85f;
        private float staminaCostScale = 1f;
        private float perceptionRange = 48f;
        private float explosionFeedbackRange = 64f;
        private float suppressionRecoveryPerTick = 0.015f;
        private float weightOrangeRatio = 0.75f;
        private float weightRedRatio = 1.15f;

        public Builder masterEnabled(boolean value) { this.masterEnabled = value; return this; }
        public Builder locomotionEnabled(boolean value) { this.locomotionEnabled = value; return this; }
        public Builder emotesEnabled(boolean value) { this.emotesEnabled = value; return this; }
        public Builder perceptionEnabled(boolean value) { this.perceptionEnabled = value; return this; }
        public Builder combatFeedbackEnabled(boolean value) { this.combatFeedbackEnabled = value; return this; }
        public Builder explosionFeedbackEnabled(boolean value) { this.explosionFeedbackEnabled = value; return this; }
        public Builder weightEnabled(boolean value) { this.weightEnabled = value; return this; }
        public Builder inputUiEnabled(boolean value) { this.inputUiEnabled = value; return this; }
        public Builder maxStamina(float value) { this.maxStamina = value; return this; }
        public Builder staminaRecoveryPerTick(float value) { this.staminaRecoveryPerTick = value; return this; }
        public Builder staminaCostScale(float value) { this.staminaCostScale = value; return this; }
        public Builder perceptionRange(float value) { this.perceptionRange = value; return this; }
        public Builder explosionFeedbackRange(float value) { this.explosionFeedbackRange = value; return this; }
        public Builder suppressionRecoveryPerTick(float value) { this.suppressionRecoveryPerTick = value; return this; }
        public Builder weightOrangeRatio(float value) { this.weightOrangeRatio = value; return this; }
        public Builder weightRedRatio(float value) { this.weightRedRatio = value; return this; }

        public GameplayRuntimeConfig build() {
            return new GameplayRuntimeConfig(
                    this.masterEnabled,
                    this.locomotionEnabled,
                    this.emotesEnabled,
                    this.perceptionEnabled,
                    this.combatFeedbackEnabled,
                    this.explosionFeedbackEnabled,
                    this.weightEnabled,
                    this.inputUiEnabled,
                    this.maxStamina,
                    this.staminaRecoveryPerTick,
                    this.staminaCostScale,
                    this.perceptionRange,
                    this.explosionFeedbackRange,
                    this.suppressionRecoveryPerTick,
                    this.weightOrangeRatio,
                    this.weightRedRatio
            );
        }
    }
}
