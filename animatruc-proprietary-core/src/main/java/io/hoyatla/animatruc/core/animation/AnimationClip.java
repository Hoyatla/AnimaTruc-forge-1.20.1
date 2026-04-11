package io.hoyatla.animatruc.core.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AnimationClip {
    private final String name;
    private final float lengthTicks;
    private final boolean looping;
    private final Map<String, BoneAnimationTrack> tracksByBone;

    public AnimationClip(String name, float lengthTicks, boolean looping, Map<String, BoneAnimationTrack> tracksByBone) {
        this.name = Objects.requireNonNull(name, "name");
        this.lengthTicks = Math.max(0f, lengthTicks);
        this.looping = looping;
        this.tracksByBone = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(tracksByBone, "tracksByBone")));
    }

    public String name() {
        return this.name;
    }

    public float lengthTicks() {
        return this.lengthTicks;
    }

    public boolean looping() {
        return this.looping;
    }

    public Set<String> animatedBones() {
        return this.tracksByBone.keySet();
    }

    public BoneAnimationTrack track(String boneName) {
        return this.tracksByBone.get(boneName);
    }
}
