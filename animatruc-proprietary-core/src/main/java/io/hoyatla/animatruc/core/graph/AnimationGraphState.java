package io.hoyatla.animatruc.core.graph;

import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.animation.BoneMask;

import java.util.Objects;

public final class AnimationGraphState {
    private final String id;
    private final AnimationClip clip;
    private final BoneMask boneMask;
    private final boolean additive;
    private final float baseWeight;

    public AnimationGraphState(String id, AnimationClip clip, BoneMask boneMask, boolean additive, float baseWeight) {
        this.id = Objects.requireNonNull(id, "id");
        this.clip = Objects.requireNonNull(clip, "clip");
        this.boneMask = Objects.requireNonNull(boneMask, "boneMask");
        this.additive = additive;
        this.baseWeight = Math.max(0f, baseWeight);
    }

    public static AnimationGraphState of(String id, AnimationClip clip) {
        return new AnimationGraphState(id, clip, BoneMask.FULL_BODY, false, 1f);
    }

    public String id() {
        return this.id;
    }

    public AnimationClip clip() {
        return this.clip;
    }

    public BoneMask boneMask() {
        return this.boneMask;
    }

    public boolean additive() {
        return this.additive;
    }

    public float baseWeight() {
        return this.baseWeight;
    }

    public AnimationGraphState withMask(BoneMask mask) {
        return new AnimationGraphState(this.id, this.clip, mask, this.additive, this.baseWeight);
    }

    public AnimationGraphState withAdditive(boolean additive) {
        return new AnimationGraphState(this.id, this.clip, this.boneMask, additive, this.baseWeight);
    }

    public AnimationGraphState withBaseWeight(float weight) {
        return new AnimationGraphState(this.id, this.clip, this.boneMask, this.additive, weight);
    }
}
