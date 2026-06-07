package io.hoyatla.animatruc.core.skinning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * CPU-skinned geometry snapshot for one sampled pose.
 */
public final class SkinnedGeometrySnapshot {
    private final List<BakedTriangle> triangles;

    public SkinnedGeometrySnapshot(List<BakedTriangle> triangles) {
        this.triangles = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(triangles, "triangles")));
    }

    public List<BakedTriangle> triangles() {
        return this.triangles;
    }

    public boolean isEmpty() {
        return this.triangles.isEmpty();
    }
}
