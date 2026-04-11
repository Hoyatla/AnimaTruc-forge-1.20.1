package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.animation.AnimationLayer;
import io.hoyatla.animatruc.core.animation.AnimationMixer;
import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.ClipState;
import io.hoyatla.animatruc.core.graph.AnimationGraphRuntime;
import io.hoyatla.animatruc.core.modifier.AnimationModifier;
import io.hoyatla.animatruc.core.profiling.AnimationRuntimeProfiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateful per-entity animator instance with adaptive update cadence.
 */
public final class AnimatorInstance {
    private final AdaptiveUpdatePolicy updatePolicy;
    private final AnimationMixer mixer;
    private final List<AnimationLayer> manualLayers = new ArrayList<>();
    private final List<AnimationModifier> modifiers = new ArrayList<>();
    private final AnimationRuntimeProfiler profiler = new AnimationRuntimeProfiler();

    private AnimationGraphRuntime graphRuntime;
    private int tickCounter = 0;
    private AnimationPose cachedPose = AnimationPose.of(Collections.emptyMap());

    public AnimatorInstance(AdaptiveUpdatePolicy updatePolicy) {
        this.updatePolicy = updatePolicy;
        this.mixer = new AnimationMixer();
    }

    public void play(ClipState state) {
        this.manualLayers.add(AnimationLayer.of(state));
    }

    public void play(AnimationLayer layer) {
        this.manualLayers.add(layer);
    }

    public void clearLayers() {
        this.manualLayers.clear();
        this.cachedPose = AnimationPose.of(Collections.emptyMap());
    }

    public void setGraphRuntime(AnimationGraphRuntime graphRuntime) {
        this.graphRuntime = graphRuntime;
    }

    public AnimationGraphRuntime graphRuntime() {
        return this.graphRuntime;
    }

    public void clearGraphRuntime() {
        this.graphRuntime = null;
    }

    public void addModifier(AnimationModifier modifier) {
        this.modifiers.add(modifier);
    }

    public void clearModifiers() {
        this.modifiers.clear();
    }

    public AnimationRuntimeProfiler profiler() {
        return this.profiler;
    }

    public AnimatorResult update(AnimatorContext context, float deltaTicks) {
        long frameStart = System.nanoTime();
        int interval = this.updatePolicy.intervalFor(
                context.distanceToCamera(),
                context.visible(),
                context.forceTick()
        );

        this.tickCounter++;

        if (!context.forceTick() && this.tickCounter % interval != 0) {
            this.profiler.record(0L, 0L, 0L, 0L, false);
            return new AnimatorResult(this.cachedPose, false);
        }

        float safeDelta = Math.max(0f, deltaTicks);
        List<AnimationLayer> evaluationLayers = new ArrayList<>(this.manualLayers.size() + 2);

        long graphStart = System.nanoTime();
        evaluationLayers.addAll(this.manualLayers);

        if (this.graphRuntime != null)
            evaluationLayers.addAll(this.graphRuntime.update(context, safeDelta));

        long graphNanos = System.nanoTime() - graphStart;

        long mixStart = System.nanoTime();
        AnimationPose sampledPose = this.mixer.sampleLayers(evaluationLayers, safeDelta);
        long mixNanos = System.nanoTime() - mixStart;

        long modifierStart = System.nanoTime();
        AnimationPose finalPose = sampledPose;

        if (!this.modifiers.isEmpty()) {
            AnimationPose.MutablePose mutablePose = sampledPose.mutableCopy();

            for (AnimationModifier modifier : this.modifiers) {
                modifier.apply(mutablePose, context, safeDelta);
            }

            finalPose = mutablePose.toImmutable();
        }

        long modifierNanos = System.nanoTime() - modifierStart;

        this.cachedPose = finalPose;

        long totalNanos = System.nanoTime() - frameStart;
        this.profiler.record(graphNanos, mixNanos, modifierNanos, totalNanos, true);

        return new AnimatorResult(finalPose, true);
    }
}
