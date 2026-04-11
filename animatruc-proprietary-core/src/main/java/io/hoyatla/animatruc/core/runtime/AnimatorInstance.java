package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.animation.AnimationMixer;
import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.ClipState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateful per-entity animator instance with adaptive update cadence.
 */
public final class AnimatorInstance {
    private final AdaptiveUpdatePolicy updatePolicy;
    private final AnimationMixer mixer;
    private final List<ClipState> layers = new ArrayList<>();
    private int tickCounter = 0;
    private AnimationPose cachedPose = AnimationPose.of(Collections.emptyMap());

    public AnimatorInstance(AdaptiveUpdatePolicy updatePolicy) {
        this.updatePolicy = updatePolicy;
        this.mixer = new AnimationMixer();
    }

    public void play(ClipState state) {
        this.layers.add(state);
    }

    public void clearLayers() {
        this.layers.clear();
        this.cachedPose = AnimationPose.of(Collections.emptyMap());
    }

    public AnimatorResult update(AnimatorContext context, float deltaTicks) {
        int interval = this.updatePolicy.intervalFor(
                context.distanceToCamera(),
                context.visible(),
                context.forceTick()
        );

        this.tickCounter++;

        if (!context.forceTick() && this.tickCounter % interval != 0)
            return new AnimatorResult(this.cachedPose, false);

        this.cachedPose = this.mixer.sample(this.layers, Math.max(0f, deltaTicks));

        return new AnimatorResult(this.cachedPose, true);
    }
}
