package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Geometry payload for one imported animation pack.
 */
public final class ModelGeometry {
    public static final ModelGeometry EMPTY = new ModelGeometry(List.of());

    private final List<ModelCube> cubes;

    public ModelGeometry(List<ModelCube> cubes) {
        this.cubes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(cubes, "cubes")));
    }

    public List<ModelCube> cubes() {
        return this.cubes;
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }
}
