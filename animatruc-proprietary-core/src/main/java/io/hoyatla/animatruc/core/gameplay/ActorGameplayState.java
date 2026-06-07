package io.hoyatla.animatruc.core.gameplay;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.Optional;

public final class ActorGameplayState {
    private final String actorId;
    private float stamina;
    private GameplayAction activeAction;
    private String activeEmote = "";
    private float alertLevel;
    private float visibility;
    private float suppression;
    private float weightRatio;
    private Vec3f lastKnownStimulus = Vec3f.ZERO;

    ActorGameplayState(String actorId, GameplayRuntimeConfig config) {
        this.actorId = actorId;
        this.stamina = config.maxStamina();
    }

    public String actorId() {
        return this.actorId;
    }

    public float stamina() {
        return this.stamina;
    }

    public Optional<GameplayAction> activeAction() {
        return Optional.ofNullable(this.activeAction);
    }

    public String activeEmote() {
        return this.activeEmote;
    }

    public float alertLevel() {
        return this.alertLevel;
    }

    public float visibility() {
        return this.visibility;
    }

    public float suppression() {
        return this.suppression;
    }

    public float weightRatio() {
        return this.weightRatio;
    }

    public Vec3f lastKnownStimulus() {
        return this.lastKnownStimulus;
    }

    void setActiveAction(GameplayAction action) {
        this.activeAction = action;
    }

    void setActiveEmote(String value) {
        this.activeEmote = value == null ? "" : value;
    }

    void setLastKnownStimulus(Vec3f value) {
        this.lastKnownStimulus = value == null ? Vec3f.ZERO : value;
    }

    void consumeStamina(float amount) {
        this.stamina = clamp(this.stamina - Math.max(0f, amount), 0f, Float.MAX_VALUE);
    }

    void recoverStamina(float amount, float maxStamina) {
        this.stamina = clamp(this.stamina + Math.max(0f, amount), 0f, Math.max(1f, maxStamina));
    }

    void addAlert(float amount) {
        this.alertLevel = clamp01(this.alertLevel + Math.max(0f, amount));
    }

    void decayAlert(float amount) {
        this.alertLevel = clamp01(this.alertLevel - Math.max(0f, amount));
    }

    void setVisibility(float value) {
        this.visibility = clamp01(value);
    }

    void addSuppression(float amount) {
        this.suppression = clamp01(this.suppression + Math.max(0f, amount));
    }

    void decaySuppression(float amount) {
        this.suppression = clamp01(this.suppression - Math.max(0f, amount));
    }

    void setWeightRatio(float value) {
        this.weightRatio = Math.max(0f, value);
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        if (value <= min)
            return min;
        if (value >= max)
            return max;
        return value;
    }
}
