package io.hoyatla.animatruc.core.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Timeline sampler for one typed animation stream.
 */
public final class AnimationChannel<T> {
    private final List<Keyframe<T>> keyframes;
    private final Interpolator<T> interpolator;
    private final T defaultValue;

    public AnimationChannel(List<Keyframe<T>> keyframes, Interpolator<T> interpolator, T defaultValue) {
        this.keyframes = new ArrayList<>(Objects.requireNonNull(keyframes, "keyframes"));
        this.keyframes.sort(Comparator.comparing(Keyframe::tick));
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    public T sample(float rawTick, float clipLengthTicks, boolean looping) {
        if (this.keyframes.isEmpty())
            return this.defaultValue;
        if (this.keyframes.size() == 1)
            return this.keyframes.get(0).value();

        float tick = normalizeTick(rawTick, clipLengthTicks, looping);
        int nextIndex = findNextKeyframeIndex(tick);

        if (nextIndex <= 0)
            return this.keyframes.get(0).value();
        if (nextIndex >= this.keyframes.size())
            return this.keyframes.get(this.keyframes.size() - 1).value();

        Keyframe<T> previous = this.keyframes.get(nextIndex - 1);
        Keyframe<T> next = this.keyframes.get(nextIndex);

        if (previous.interpolationMode() == InterpolationMode.STEP)
            return previous.value();

        float span = next.tick() - previous.tick();

        if (span <= 0f)
            return next.value();

        float alpha = (tick - previous.tick()) / span;

        return this.interpolator.interpolate(previous.value(), next.value(), alpha);
    }

    private int findNextKeyframeIndex(float tick) {
        int low = 0;
        int high = this.keyframes.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            float midTick = this.keyframes.get(mid).tick();

            if (tick < midTick) {
                high = mid - 1;
            }
            else {
                low = mid + 1;
            }
        }

        return low;
    }

    private static float normalizeTick(float tick, float clipLength, boolean looping) {
        if (!looping || clipLength <= 0f)
            return Math.max(0f, tick);

        float wrapped = tick % clipLength;

        return wrapped < 0f ? wrapped + clipLength : wrapped;
    }
}
