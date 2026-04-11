package io.hoyatla.animatruc.core.animation;

import java.util.Objects;

/**
 * A clip instance plus layer-specific blend metadata.
 */
public final class AnimationLayer {
    private final ClipState clipState;
    private final BoneMask boneMask;
    private final float layerWeight;
    private final boolean enabled;

    private AnimationLayer(ClipState clipState, BoneMask boneMask, float layerWeight, boolean enabled) {
        this.clipState = Objects.requireNonNull(clipState, "clipState");
        this.boneMask = Objects.requireNonNull(boneMask, "boneMask");
        this.layerWeight = Math.max(0f, layerWeight);
        this.enabled = enabled;
    }

    public static AnimationLayer of(ClipState clipState) {
        return new AnimationLayer(clipState, BoneMask.FULL_BODY, 1f, true);
    }

    public ClipState clipState() {
        return this.clipState;
    }

    public BoneMask boneMask() {
        return this.boneMask;
    }

    public float layerWeight() {
        return this.layerWeight;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public AnimationLayer withMask(BoneMask mask) {
        return new AnimationLayer(this.clipState, mask, this.layerWeight, this.enabled);
    }

    public AnimationLayer withLayerWeight(float weight) {
        return new AnimationLayer(this.clipState, this.boneMask, weight, this.enabled);
    }

    public AnimationLayer withEnabled(boolean enabled) {
        return new AnimationLayer(this.clipState, this.boneMask, this.layerWeight, enabled);
    }
}
