package io.hoyatla.animatruc.core.animation;

import java.util.Objects;

public final class ClipState {
    private final AnimationClip clip;
    private final boolean additive;
    private float localTick;
    private float weight;
    private boolean active;

    public ClipState(AnimationClip clip, float weight, boolean additive) {
        this.clip = Objects.requireNonNull(clip, "clip");
        this.weight = Math.max(0f, weight);
        this.additive = additive;
        this.active = true;
    }

    public AnimationClip clip() {
        return this.clip;
    }

    public float localTick() {
        return this.localTick;
    }

    public float weight() {
        return this.weight;
    }

    public boolean additive() {
        return this.additive;
    }

    public boolean active() {
        return this.active;
    }

    public void setWeight(float weight) {
        this.weight = Math.max(0f, weight);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void advance(float deltaTicks) {
        if (!this.active)
            return;

        this.localTick += Math.max(0f, deltaTicks);

        if (!this.clip.looping() && this.localTick > this.clip.lengthTicks()) {
            this.localTick = this.clip.lengthTicks();
            this.active = false;
        }
    }
}
