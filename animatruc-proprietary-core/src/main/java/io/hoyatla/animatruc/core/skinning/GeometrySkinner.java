package io.hoyatla.animatruc.core.skinning;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.asset.ModelBone;
import io.hoyatla.animatruc.core.asset.ModelCube;
import io.hoyatla.animatruc.core.asset.ModelGeometry;
import io.hoyatla.animatruc.core.asset.ModelMesh;
import io.hoyatla.animatruc.core.asset.ModelMeshFace;
import io.hoyatla.animatruc.core.asset.ModelMeshSkin;
import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.asset.ModelUv;
import io.hoyatla.animatruc.core.asset.VertexInfluence;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves one sampled pose into CPU-skinned triangles for Forge-side rendering.
 */
public final class GeometrySkinner {
    private static final ModelUv UV00 = new ModelUv(0f, 0f);
    private static final ModelUv UV10 = new ModelUv(1f, 0f);
    private static final ModelUv UV11 = new ModelUv(1f, 1f);
    private static final ModelUv UV01 = new ModelUv(0f, 1f);

    public SkinnedGeometrySnapshot skin(AnimationAssetPack pack, AnimationPose pose) {
        return skin(pack.skeleton(), pack.geometry(), pose);
    }

    public SkinnedGeometrySnapshot skin(ModelSkeleton skeleton, ModelGeometry geometry, AnimationPose pose) {
        BonePalette palette = BonePalette.build(skeleton, pose);
        List<BakedTriangle> triangles = new ArrayList<>();

        for (ModelMesh mesh : geometry.meshes()) {
            emitMesh(mesh, palette, triangles);
        }

        for (ModelCube cube : geometry.cubes()) {
            emitCube(cube, palette, triangles);
        }

        return new SkinnedGeometrySnapshot(triangles);
    }

    private static void emitMesh(ModelMesh mesh, BonePalette palette, List<BakedTriangle> destination) {
        List<Vec3f> skinnedVertices = new ArrayList<>(mesh.vertices().size());
        for (int index = 0; index < mesh.vertices().size(); index++) {
            skinnedVertices.add(skinMeshVertex(mesh, index, palette));
        }

        for (ModelMeshFace face : mesh.faces()) {
            List<Integer> indices = face.vertexIndices();

            if (indices.size() < 3)
                continue;

            for (int triangleIndex = 1; triangleIndex < indices.size() - 1; triangleIndex++) {
                int index0 = indices.get(0);
                int index1 = indices.get(triangleIndex);
                int index2 = indices.get(triangleIndex + 1);

                if (index0 >= skinnedVertices.size() || index1 >= skinnedVertices.size() || index2 >= skinnedVertices.size())
                    continue;

                Vec3f p0 = skinnedVertices.get(index0);
                Vec3f p1 = skinnedVertices.get(index1);
                Vec3f p2 = skinnedVertices.get(index2);
                Vec3f normal = p1.subtract(p0).cross(p2.subtract(p0)).normalize();

                destination.add(new BakedTriangle(
                        mesh.materialName(),
                        new BakedVertex(p0, normal, uvFor(face.uvs(), 0)),
                        new BakedVertex(p1, normal, uvFor(face.uvs(), triangleIndex)),
                        new BakedVertex(p2, normal, uvFor(face.uvs(), triangleIndex + 1))
                ));
            }
        }
    }

    private static Vec3f skinMeshVertex(ModelMesh mesh, int vertexIndex, BonePalette palette) {
        Vec3f baseVertex = mesh.vertices().get(vertexIndex);
        ModelMeshSkin skin = mesh.skin();

        if (skin == null || skin.influencesByVertex().size() <= vertexIndex) {
            return palette.transformRigid(mesh.boneName(), mesh.origin().add(baseVertex));
        }

        List<VertexInfluence> influences = skin.influencesByVertex().get(vertexIndex);

        if (influences.isEmpty()) {
            Vec3f rest = skin.modelSpaceVertices() ? baseVertex : mesh.origin().add(baseVertex);
            return palette.transformRigid(mesh.boneName(), rest);
        }

        Vec3f rest = skin.modelSpaceVertices() ? baseVertex : mesh.origin().add(baseVertex);
        return palette.transformSmooth(rest, influences);
    }

    private static void emitCube(ModelCube cube, BonePalette palette, List<BakedTriangle> destination) {
        Vec3f from = cube.from();
        Vec3f to = cube.to();
        Vec3f[] vertices = new Vec3f[]{
                new Vec3f(from.x(), from.y(), from.z()),
                new Vec3f(to.x(), from.y(), from.z()),
                new Vec3f(from.x(), to.y(), from.z()),
                new Vec3f(to.x(), to.y(), from.z()),
                new Vec3f(from.x(), from.y(), to.z()),
                new Vec3f(to.x(), from.y(), to.z()),
                new Vec3f(from.x(), to.y(), to.z()),
                new Vec3f(to.x(), to.y(), to.z())
        };

        for (int index = 0; index < vertices.length; index++) {
            vertices[index] = palette.transformRigid(cube.boneName(), vertices[index]);
        }

        emitQuad(destination, cube.materialName(), vertices[0], vertices[1], vertices[3], vertices[2]);
        emitQuad(destination, cube.materialName(), vertices[4], vertices[6], vertices[7], vertices[5]);
        emitQuad(destination, cube.materialName(), vertices[0], vertices[2], vertices[6], vertices[4]);
        emitQuad(destination, cube.materialName(), vertices[1], vertices[5], vertices[7], vertices[3]);
        emitQuad(destination, cube.materialName(), vertices[0], vertices[4], vertices[5], vertices[1]);
        emitQuad(destination, cube.materialName(), vertices[2], vertices[3], vertices[7], vertices[6]);
    }

    private static void emitQuad(List<BakedTriangle> destination, String materialName, Vec3f a, Vec3f b, Vec3f c, Vec3f d) {
        Vec3f normal = b.subtract(a).cross(c.subtract(a)).normalize();
        destination.add(new BakedTriangle(
                materialName,
                new BakedVertex(a, normal, UV00),
                new BakedVertex(b, normal, UV10),
                new BakedVertex(c, normal, UV11)
        ));
        destination.add(new BakedTriangle(
                materialName,
                new BakedVertex(a, normal, UV00),
                new BakedVertex(c, normal, UV11),
                new BakedVertex(d, normal, UV01)
        ));
    }

    private static ModelUv uvFor(List<ModelUv> uvs, int index) {
        if (index < uvs.size())
            return uvs.get(index);

        return switch (index & 3) {
            case 0 -> UV00;
            case 1 -> UV10;
            case 2 -> UV11;
            default -> UV01;
        };
    }

    private static final class BonePalette {
        private final Map<String, Mat4f> skinnedByBone;

        private BonePalette(Map<String, Mat4f> skinnedByBone) {
            this.skinnedByBone = skinnedByBone;
        }

        private static BonePalette build(ModelSkeleton skeleton, AnimationPose pose) {
            var skinnedMatrices = new java.util.LinkedHashMap<String, Mat4f>(skeleton.orderedBones().size());
            var bindMatrices = new java.util.LinkedHashMap<String, Mat4f>(skeleton.orderedBones().size());
            var globalPoseMatrices = new java.util.LinkedHashMap<String, Mat4f>(skeleton.orderedBones().size());

            for (ModelBone bone : skeleton.orderedBones()) {
                Mat4f parentBind = bone.parentName() == null ? Mat4f.IDENTITY : bindMatrices.getOrDefault(bone.parentName(), Mat4f.IDENTITY);
                Mat4f parentPose = bone.parentName() == null ? Mat4f.IDENTITY : globalPoseMatrices.getOrDefault(bone.parentName(), Mat4f.IDENTITY);
                Vec3f staticOffset = localPivotOffset(skeleton, bone);
                Transform bind = bone.bindTransform();
                Transform animated = pose.transform(bone.name());

                Mat4f localBind = Mat4f.trs(staticOffset.add(bind.translation()), bind.rotation(), bind.scale());
                Mat4f localPose = Mat4f.trs(
                        staticOffset.add(bind.translation()).add(animated.translation()),
                        bind.rotation().multiply(animated.rotation()),
                        bind.scale().multiply(animated.scale())
                );

                Mat4f globalBind = parentBind.multiply(localBind);
                Mat4f globalPose = parentPose.multiply(localPose);
                bindMatrices.put(bone.name(), globalBind);
                globalPoseMatrices.put(bone.name(), globalPose);
                skinnedMatrices.put(bone.name(), globalPose.multiply(globalBind.invertAffine()));
            }

            return new BonePalette(skinnedMatrices);
        }

        private static Vec3f localPivotOffset(ModelSkeleton skeleton, ModelBone bone) {
            if (bone.parentName() == null)
                return bone.pivot();

            ModelBone parent = skeleton.bone(bone.parentName());
            return parent == null ? bone.pivot() : bone.pivot().subtract(parent.pivot());
        }

        private Vec3f transformRigid(String boneName, Vec3f restPosition) {
            return this.skinnedByBone.getOrDefault(boneName, Mat4f.IDENTITY).transformPosition(restPosition);
        }

        private Vec3f transformSmooth(Vec3f restPosition, List<VertexInfluence> influences) {
            Vec3f accumulator = Vec3f.ZERO;
            float totalWeight = 0f;

            for (VertexInfluence influence : influences) {
                float weight = influence.weight();

                if (weight <= 0f)
                    continue;

                Mat4f matrix = this.skinnedByBone.getOrDefault(influence.boneName(), Mat4f.IDENTITY);
                accumulator = accumulator.add(matrix.transformPosition(restPosition).multiply(weight));
                totalWeight += weight;
            }

            if (totalWeight <= 0f)
                return restPosition;

            return accumulator.divide(totalWeight);
        }
    }
}
