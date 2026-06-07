package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Optional smooth skinning payload for a mesh.
 */
public final class ModelMeshSkin {
    private final boolean modelSpaceVertices;
    private final List<List<VertexInfluence>> influencesByVertex;

    public ModelMeshSkin(boolean modelSpaceVertices, List<List<VertexInfluence>> influencesByVertex) {
        this.modelSpaceVertices = modelSpaceVertices;
        Objects.requireNonNull(influencesByVertex, "influencesByVertex");

        List<List<VertexInfluence>> immutable = new ArrayList<>(influencesByVertex.size());
        for (List<VertexInfluence> influences : influencesByVertex) {
            immutable.add(Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(influences, "influences"))));
        }

        this.influencesByVertex = Collections.unmodifiableList(immutable);
    }

    public boolean modelSpaceVertices() {
        return this.modelSpaceVertices;
    }

    public List<List<VertexInfluence>> influencesByVertex() {
        return this.influencesByVertex;
    }

    public boolean isEmpty() {
        return this.influencesByVertex.isEmpty();
    }
}
