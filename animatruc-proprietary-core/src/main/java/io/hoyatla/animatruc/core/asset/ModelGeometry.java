package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Geometry payload for one imported animation pack.
 */
public final class ModelGeometry {
    public static final ModelGeometry EMPTY = new ModelGeometry(List.of(), List.of());

    private final List<ModelCube> cubes;
    private final List<ModelMesh> meshes;

    public ModelGeometry(List<ModelCube> cubes) {
        this(cubes, List.of());
    }

    public ModelGeometry(List<ModelCube> cubes, List<ModelMesh> meshes) {
        this.cubes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cubes, "cubes")));
        this.meshes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(meshes, "meshes")));
    }

    public List<ModelCube> cubes() {
        return this.cubes;
    }

    public List<ModelMesh> meshes() {
        return this.meshes;
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty() && this.meshes.isEmpty();
    }
}
