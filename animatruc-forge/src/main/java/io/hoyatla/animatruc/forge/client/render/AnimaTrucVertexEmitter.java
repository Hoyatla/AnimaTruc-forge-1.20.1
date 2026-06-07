package io.hoyatla.animatruc.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.hoyatla.animatruc.core.skinning.BakedTriangle;
import io.hoyatla.animatruc.core.skinning.BakedVertex;
import io.hoyatla.animatruc.core.skinning.SkinnedGeometrySnapshot;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Minimal Forge-side bridge from AnimaTruc CPU-skinned triangles to Minecraft vertex consumers.
 */
public final class AnimaTrucVertexEmitter {
    private AnimaTrucVertexEmitter() {
    }

    public static void emit(
            SkinnedGeometrySnapshot snapshot,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (BakedTriangle triangle : snapshot.triangles()) {
            emitVertex(vertexConsumer, poseMatrix, normalMatrix, triangle.a(), packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(vertexConsumer, poseMatrix, normalMatrix, triangle.b(), packedLight, packedOverlay, red, green, blue, alpha);
            emitVertex(vertexConsumer, poseMatrix, normalMatrix, triangle.c(), packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static void emitVertex(
            VertexConsumer vertexConsumer,
            Matrix4f poseMatrix,
            Matrix3f normalMatrix,
            BakedVertex vertex,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        vertexConsumer.vertex(poseMatrix, vertex.position().x(), vertex.position().y(), vertex.position().z())
                .color(red, green, blue, alpha)
                .uv(vertex.uv().u(), vertex.uv().v())
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normalMatrix, vertex.normal().x(), vertex.normal().y(), vertex.normal().z())
                .endVertex();
    }
}
