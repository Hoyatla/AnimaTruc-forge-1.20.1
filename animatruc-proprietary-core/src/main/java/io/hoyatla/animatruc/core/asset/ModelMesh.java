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
    private final String materialName;
    private final Vec3f origin;
    private final List<Vec3f> vertices;
    private final List<ModelMeshFace> faces;
    private final ModelMeshSkin skin;

    public ModelMesh(String name, String boneName, Vec3f origin, List<Vec3f> vertices, List<ModelMeshFace> faces) {
        this(name, boneName, null, origin, vertices, faces, null);
    }

    public ModelMesh(
            String name,
            String boneName,
            String materialName,
            Vec3f origin,
            List<Vec3f> vertices,
            List<ModelMeshFace> faces,
            ModelMeshSkin skin) {
        this.name = Objects.requireNonNull(name, "name");
        this.boneName = Objects.requireNonNull(boneName, "boneName");
        this.materialName = materialName;
        this.origin = Objects.requireNonNull(origin, "origin");
        this.vertices = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(vertices, "vertices")));
        this.faces = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(faces, "faces")));
        this.skin = skin;
    }

    public String name() {
        return this.name;
    }

    public String boneName() {
        return this.boneName;
    }

    public String materialName() {
        return this.materialName;
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

    public ModelMeshSkin skin() {
        return this.skin;
    }

    public boolean isSkinned() {
        return this.skin != null && !this.skin.isEmpty();
    }
}
