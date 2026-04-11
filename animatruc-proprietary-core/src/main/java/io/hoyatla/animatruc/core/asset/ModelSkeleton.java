package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered skeleton definition with parent linkage validation.
 */
public final class ModelSkeleton {
    private final Map<String, ModelBone> bonesByName;
    private final List<ModelBone> orderedBones;

    public ModelSkeleton(List<ModelBone> orderedBones) {
        Objects.requireNonNull(orderedBones, "orderedBones");

        Map<String, ModelBone> byName = new LinkedHashMap<>(orderedBones.size());

        for (ModelBone bone : orderedBones) {
            if (byName.putIfAbsent(bone.name(), bone) != null)
                throw new IllegalArgumentException("Duplicate bone name: " + bone.name());
        }

        for (ModelBone bone : orderedBones) {
            String parentName = bone.parentName();

            if (parentName != null && !parentName.isEmpty() && !byName.containsKey(parentName))
                throw new IllegalArgumentException("Unknown parent bone '" + parentName + "' for bone '" + bone.name() + "'");
        }

        this.bonesByName = Collections.unmodifiableMap(byName);
        this.orderedBones = Collections.unmodifiableList(new ArrayList<>(orderedBones));
    }

    public List<ModelBone> orderedBones() {
        return this.orderedBones;
    }

    public ModelBone bone(String name) {
        return this.bonesByName.get(name);
    }

    public boolean containsBone(String name) {
        return this.bonesByName.containsKey(name);
    }

    public Map<String, ModelBone> bonesByName() {
        return this.bonesByName;
    }
}
