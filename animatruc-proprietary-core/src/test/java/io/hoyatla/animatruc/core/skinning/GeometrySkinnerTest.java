package io.hoyatla.animatruc.core.skinning;

import io.hoyatla.animatruc.core.animation.AnimationPose;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.asset.ModelBone;
import io.hoyatla.animatruc.core.asset.ModelGeometry;
import io.hoyatla.animatruc.core.asset.ModelMesh;
import io.hoyatla.animatruc.core.asset.ModelMeshFace;
import io.hoyatla.animatruc.core.asset.ModelMeshSkin;
import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.asset.ModelUv;
import io.hoyatla.animatruc.core.asset.VertexInfluence;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeometrySkinnerTest {
    @Test
    void shouldSkinSmoothMeshVerticesAgainstBonePose() {
        ModelSkeleton skeleton = new ModelSkeleton(List.of(
                new ModelBone("root", null, Vec3f.ZERO, Transform.IDENTITY),
                new ModelBone("arm", "root", new Vec3f(2f, 0f, 0f), Transform.IDENTITY)
        ));

        ModelMesh mesh = new ModelMesh(
                "arm_mesh",
                "root",
                "hero_material",
                Vec3f.ZERO,
                List.of(
                        new Vec3f(0f, 0f, 0f),
                        new Vec3f(2f, 0f, 0f),
                        new Vec3f(2f, 1f, 0f)
                ),
                List.of(new ModelMeshFace(List.of(0, 1, 2), List.of(
                        new ModelUv(0f, 0f),
                        new ModelUv(1f, 0f),
                        new ModelUv(1f, 1f)
                ))),
                new ModelMeshSkin(true, List.of(
                        List.of(new VertexInfluence("root", 1f)),
                        List.of(new VertexInfluence("root", 0.25f), new VertexInfluence("arm", 0.75f)),
                        List.of(new VertexInfluence("arm", 1f))
                ))
        );

        AnimationAssetPack pack = new AnimationAssetPack(
                skeleton,
                new ModelGeometry(List.of(), List.of(mesh)),
                Map.of()
        );

        AnimationPose pose = AnimationPose.of(Map.of(
                "arm",
                new Transform(new Vec3f(1f, 0f, 0f), Quatf.IDENTITY, Vec3f.ONE)
        ));

        SkinnedGeometrySnapshot snapshot = new GeometrySkinner().skin(pack, pose);

        assertFalse(snapshot.isEmpty());
        assertEquals(1, snapshot.triangles().size());
        assertEquals(0f, snapshot.triangles().get(0).a().position().x(), 0.0001f);
        assertEquals(2.75f, snapshot.triangles().get(0).b().position().x(), 0.0001f);
        assertEquals(3f, snapshot.triangles().get(0).c().position().x(), 0.0001f);
        assertEquals("hero_material", snapshot.triangles().get(0).materialName());
    }
}
