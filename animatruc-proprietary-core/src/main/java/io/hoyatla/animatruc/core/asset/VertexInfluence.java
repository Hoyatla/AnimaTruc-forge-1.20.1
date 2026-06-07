package io.hoyatla.animatruc.core.asset;

import java.util.Objects;

/**
 * One bone influence for a skinned mesh vertex.
 */
public final class VertexInfluence {
    private final String boneName;
    private final float weight;

    public VertexInfluence(String boneName, float weight) {
        this.boneName = Objects.requireNonNull(boneName, "boneName");
        this.weight = weight;
    }

    public String boneName() {
        return this.boneName;
    }

    public float weight() {
        return this.weight;
    }
}
