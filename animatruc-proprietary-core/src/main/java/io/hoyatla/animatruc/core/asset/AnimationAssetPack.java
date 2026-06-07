package io.hoyatla.animatruc.core.asset;

import io.hoyatla.animatruc.core.animation.AnimationClip;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Imported animation data package containing skeleton and named clips.
 */
public final class AnimationAssetPack {
    private final PackMetadata metadata;
    private final ModelSkeleton skeleton;
    private final ModelGeometry geometry;
    private final Map<String, AnimationClip> clipsByName;

    public AnimationAssetPack(ModelSkeleton skeleton, Map<String, AnimationClip> clipsByName) {
        this(PackMetadata.EMPTY, skeleton, ModelGeometry.EMPTY, clipsByName);
    }

    public AnimationAssetPack(ModelSkeleton skeleton, ModelGeometry geometry, Map<String, AnimationClip> clipsByName) {
        this(PackMetadata.EMPTY, skeleton, geometry, clipsByName);
    }

    public AnimationAssetPack(PackMetadata metadata, ModelSkeleton skeleton, ModelGeometry geometry, Map<String, AnimationClip> clipsByName) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.skeleton = Objects.requireNonNull(skeleton, "skeleton");
        this.geometry = Objects.requireNonNull(geometry, "geometry");
        this.clipsByName = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(clipsByName, "clipsByName")));
    }

    public PackMetadata metadata() {
        return this.metadata;
    }

    public ModelSkeleton skeleton() {
        return this.skeleton;
    }

    public ModelGeometry geometry() {
        return this.geometry;
    }

    public Map<String, AnimationClip> clipsByName() {
        return this.clipsByName;
    }

    public AnimationClip clip(String name) {
        return this.clipsByName.get(name);
    }
}
