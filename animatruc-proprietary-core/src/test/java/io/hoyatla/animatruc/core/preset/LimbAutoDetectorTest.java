package io.hoyatla.animatruc.core.preset;

import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.asset.ModelBone;
import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.math.Vec3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimbAutoDetectorTest {
    @Test
    void shouldDetectBipedLegsWithoutArms() {
        ModelSkeleton skeleton = new ModelSkeleton(List.of(
                bone("root", null, 0f, 0f, 0f),
                bone("pelvis", "root", 0f, 12f, 0f),
                bone("left_upper_leg", "pelvis", -2f, 10f, 0f),
                bone("left_lower_leg", "left_upper_leg", -2f, 6f, 1f),
                bone("left_foot", "left_lower_leg", -2f, 0f, 2f),
                bone("right_upper_leg", "pelvis", 2f, 10f, 0f),
                bone("right_lower_leg", "right_upper_leg", 2f, 6f, 1f),
                bone("right_foot", "right_lower_leg", 2f, 0f, 2f),
                bone("left_arm", "root", -4f, 13f, 0f),
                bone("left_forearm", "left_arm", -6f, 12f, 0f),
                bone("left_hand", "left_forearm", -7f, 11.5f, 0f)
        ));

        LimbDetectionReport report = LimbAutoDetector.detect(skeleton, LimbDetectionOptions.DEFAULT);

        assertEquals(LocomotionPresetType.BIPED, report.presetType());
        assertTrue(report.accepted());
        assertEquals(2, report.chains().size());
    }

    @Test
    void shouldDetectHexapodByLegCount() {
        List<ModelBone> bones = new ArrayList<>();
        bones.add(bone("root", null, 0f, 0f, 0f));
        bones.add(bone("thorax", "root", 0f, 8f, 0f));

        bones.addAll(hexLeg("front_left", "thorax", -4f, 1f));
        bones.addAll(hexLeg("mid_left", "thorax", -4f, 0f));
        bones.addAll(hexLeg("rear_left", "thorax", -4f, -1f));
        bones.addAll(hexLeg("front_right", "thorax", 4f, 1f));
        bones.addAll(hexLeg("mid_right", "thorax", 4f, 0f));
        bones.addAll(hexLeg("rear_right", "thorax", 4f, -1f));

        LimbDetectionReport report = LimbAutoDetector.detect(new ModelSkeleton(bones), LimbDetectionOptions.DEFAULT);

        assertEquals(LocomotionPresetType.HEXAPOD, report.presetType());
        assertTrue(report.accepted());
        assertEquals(6, report.chains().size());
    }

    @Test
    void shouldTrimMyriapodByOption() {
        List<ModelBone> bones = new ArrayList<>();
        bones.add(bone("root", null, 0f, 0f, 0f));
        bones.add(bone("body", "root", 0f, 6f, 0f));

        for (int i = 0; i < 12; i++) {
            float x = i % 2 == 0 ? -3f : 3f;
            float z = -3f + i * 0.6f;
            bones.addAll(hexLeg("segment_" + i + (x < 0f ? "_left" : "_right"), "body", x, z));
        }

        LimbDetectionOptions options = LimbDetectionOptions.builder()
                .maxMyriapodChains(10)
                .build();
        LimbDetectionReport report = LimbAutoDetector.detect(new ModelSkeleton(bones), options);

        assertEquals(LocomotionPresetType.MYRIAPOD, report.presetType());
        assertTrue(report.accepted());
        assertEquals(10, report.chains().size());
    }

    private static List<ModelBone> hexLeg(String id, String parent, float x, float z) {
        return List.of(
                bone(id + "_upper_leg", parent, x, 6f, z),
                bone(id + "_lower_leg", id + "_upper_leg", x, 3f, z + 0.3f),
                bone(id + "_foot", id + "_lower_leg", x, 0f, z + 0.6f)
        );
    }

    private static ModelBone bone(String name, String parent, float x, float y, float z) {
        return new ModelBone(name, parent, new Vec3f(x, y, z), Transform.IDENTITY);
    }
}
