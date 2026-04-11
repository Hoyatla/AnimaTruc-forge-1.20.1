package io.hoyatla.animatruc.core.animation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic layer mixer with additive support.
 */
public final class AnimationMixer {
    public AnimationPose sample(List<ClipState> states, float deltaTicks) {
        Map<String, WeightedTransformAccumulator> accumulators = new HashMap<>();

        for (ClipState state : states) {
            if (!state.active() || state.weight() <= 0f)
                continue;

            sampleStateIntoAccumulators(state, BoneMask.FULL_BODY, 1f, deltaTicks, accumulators);
        }

        return resolvePose(accumulators);
    }

    public AnimationPose sampleLayers(List<AnimationLayer> layers, float deltaTicks) {
        Map<String, WeightedTransformAccumulator> accumulators = new HashMap<>();

        for (AnimationLayer layer : layers) {
            if (!layer.enabled())
                continue;

            sampleStateIntoAccumulators(
                    layer.clipState(),
                    layer.boneMask(),
                    layer.layerWeight(),
                    deltaTicks,
                    accumulators
            );
        }

        return resolvePose(accumulators);
    }

    private static AnimationPose resolvePose(Map<String, WeightedTransformAccumulator> accumulators) {
        Map<String, Transform> pose = new HashMap<>(accumulators.size());
        for (Map.Entry<String, WeightedTransformAccumulator> entry : accumulators.entrySet()) {
            pose.put(entry.getKey(), entry.getValue().resolve());
        }

        return AnimationPose.of(pose);
    }

    private static void sampleStateIntoAccumulators(
            ClipState state,
            BoneMask mask,
            float layerWeight,
            float deltaTicks,
            Map<String, WeightedTransformAccumulator> accumulators) {
        if (!state.active() || state.weight() <= 0f || layerWeight <= 0f)
            return;

        state.advance(deltaTicks);

        AnimationClip clip = state.clip();
        float localTick = state.localTick();

        for (String bone : clip.animatedBones()) {
            float finalWeight = state.weight() * layerWeight * mask.weightFor(bone);

            if (finalWeight <= 0f)
                continue;

            BoneAnimationTrack track = clip.track(bone);
            Transform sample = track.sample(localTick, clip.lengthTicks(), clip.looping());

            WeightedTransformAccumulator accumulator = accumulators.computeIfAbsent(
                    bone,
                    unused -> WeightedTransformAccumulator.base()
            );

            if (state.additive()) {
                accumulator.addAdditive(sample, finalWeight);
            }
            else {
                accumulator.addWeighted(sample, finalWeight);
            }
        }
    }
}
