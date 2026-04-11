package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Polygon face of a mesh, with optional per-vertex UVs.
 */
public final class ModelMeshFace {
    private final List<Integer> vertexIndices;
    private final List<ModelUv> uvs;

    public ModelMeshFace(List<Integer> vertexIndices, List<ModelUv> uvs) {
        this.vertexIndices = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(vertexIndices, "vertexIndices")));
        this.uvs = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(uvs, "uvs")));
    }

    public List<Integer> vertexIndices() {
        return this.vertexIndices;
    }

    public List<ModelUv> uvs() {
        return this.uvs;
    }
}
