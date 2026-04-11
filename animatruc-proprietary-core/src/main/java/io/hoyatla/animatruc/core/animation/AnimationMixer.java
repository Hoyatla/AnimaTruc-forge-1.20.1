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

            state.advance(deltaTicks);

            AnimationClip clip = state.clip();
            float localTick = state.localTick();
            float weight = state.weight();

            for (String bone : clip.animatedBones()) {
                BoneAnimationTrack track = clip.track(bone);
                Transform sample = track.sample(localTick, clip.lengthTicks(), clip.looping());

                WeightedTransformAccumulator accumulator = accumulators.computeIfAbsent(
                        bone,
                        unused -> WeightedTransformAccumulator.base()
                );

                if (state.additive()) {
                    accumulator.addAdditive(sample, weight);
                }
                else {
                    accumulator.addWeighted(sample, weight);
                }
            }
        }

        Map<String, Transform> pose = new HashMap<>(accumulators.size());

        for (Map.Entry<String, WeightedTransformAccumulator> entry : accumulators.entrySet()) {
            pose.put(entry.getKey(), entry.getValue().resolve());
        }

        return AnimationPose.of(pose);
    }
}
