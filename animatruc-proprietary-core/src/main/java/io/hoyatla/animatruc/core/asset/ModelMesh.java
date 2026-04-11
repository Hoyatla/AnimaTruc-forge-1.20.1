package io.hoyatla.animatruc.core.asset;

import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Mesh geometry bound to one bone.
 */
public final class ModelMesh {
    private final String name;
    private final String boneName;
    private final Vec3f origin;
    private final List<Vec3f> vertices;
    private final List<ModelMeshFace> faces;

    public ModelMesh(String name, String boneName, Vec3f origin, List<Vec3f> vertices, List<ModelMeshFace> faces) {
        this.name = Objects.requireNonNull(name, "name");
        this.boneName = Objects.requireNonNull(boneName, "boneName");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.vertices = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(vertices, "vertices")));
        this.faces = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(faces, "faces")));
    }

    public String name() {
        return this.name;
    }

    public String boneName() {
        return this.boneName;
    }

    public Vec3f origin() {
        return this.origin;
    }

    public List<Vec3f> vertices() {
        return this.vertices;
    }

    public List<ModelMeshFace> faces() {
        return this.faces;
    }
}
