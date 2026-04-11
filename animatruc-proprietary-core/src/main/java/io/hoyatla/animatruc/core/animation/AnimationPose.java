package io.hoyatla.animatruc.core.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable sampled pose map by bone name.
 */
public final class AnimationPose {
    private final Map<String, Transform> bones;

    private AnimationPose(Map<String, Transform> bones) {
        this.bones = Collections.unmodifiableMap(bones);
    }

    public static AnimationPose of(Map<String, Transform> map) {
        return new AnimationPose(new HashMap<>(Objects.requireNonNull(map, "map")));
    }

    public Transform transform(String boneName) {
        return this.bones.getOrDefault(boneName, Transform.IDENTITY);
    }

    public Set<String> bones() {
        return this.bones.keySet();
    }

    public Map<String, Transform> asMap() {
        return this.bones;
    }
}
