package io.hoyatla.animatruc.forge.preset;

import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.modifier.IkChainDefinition;
import io.hoyatla.animatruc.core.modifier.IkPlane;
import io.hoyatla.animatruc.core.preset.DetectedLimbChain;
import io.hoyatla.animatruc.core.preset.LimbAutoDetector;
import io.hoyatla.animatruc.core.preset.LimbDetectionReport;
import io.hoyatla.animatruc.core.preset.LocomotionPresetType;
import io.hoyatla.animatruc.forge.config.AnimaTrucPresetConfig;
import io.hoyatla.animatruc.forge.ground.GroundContactLimbDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a ready-to-use ground-contact preset from a skeleton and current config.
 */
public final class AutoGroundContactPresetFactory {
    private AutoGroundContactPresetFactory() {
    }

    public static Optional<AutoGroundContactPreset> detect(ModelSkeleton skeleton) {
        return detect(skeleton, AnimaTrucPresetConfig.ENABLE_GROUND_RAYCAST.get());
    }

    public static Optional<AutoGroundContactPreset> detect(ModelSkeleton skeleton, boolean enableGroundRaycast) {
        Objects.requireNonNull(skeleton, "skeleton");

        if (!AnimaTrucPresetConfig.ENABLE_AUTO_DETECTION.get())
            return Optional.empty();

        LimbDetectionReport report = LimbAutoDetector.detect(skeleton, AnimaTrucPresetConfig.detectionOptions());

        if (!report.accepted() || report.chains().isEmpty())
            return Optional.empty();

        List<IkChainDefinition> ikChains = new ArrayList<>(report.chains().size());
        List<GroundContactLimbDefinition> groundLimbs = new ArrayList<>(report.chains().size());
        float speedScale = AnimaTrucPresetConfig.speedScale();
        float liftScale = AnimaTrucPresetConfig.liftScale();
        float smoothingScale = AnimaTrucPresetConfig.smoothingScale();

        for (DetectedLimbChain chain : report.chains()) {
            IkPlane plane = planeFor(chain);
            float smoothing = (2.8f + chain.confidence() * 2.0f) * smoothingScale;
            ikChains.add(IkChainDefinition.builder(
                    chain.id(),
                    chain.rootBone(),
                    chain.midBone(),
                    chain.endBone(),
                    chain.targetKey()
            ).plane(plane)
                    .lengths(chain.upperLength(), chain.lowerLength())
                    .smoothing(smoothing)
                    .weightScalarKey("ik_weight_" + chain.id())
                    .build());

            if (!enableGroundRaycast)
                continue;

            groundLimbs.add(GroundContactLimbDefinition.builder(
                    chain.id(),
                    chain.targetKey(),
                    chain.anchorLocal()
            ).phaseOffset(chain.phaseOffset())
                    .stepDistance(stepDistance(report.presetType()) * speedScale)
                    .stepTriggerDistance(stepTriggerDistance(report.presetType()) * speedScale)
                    .liftHeight(liftHeight(report.presetType()) * liftScale)
                    .probe(0.5f, probeDepth(report.presetType()))
                    .build());
        }

        if (ikChains.isEmpty())
            return Optional.empty();

        LocomotionPresetType profile = report.presetType();
        float baseSwing = switch (profile) {
            case BIPED -> 5.0f;
            case HEXAPOD -> 4.3f;
            case OCTOPOD -> 4.0f;
            case MYRIAPOD -> 3.4f;
            case UNKNOWN -> 5.0f;
        };
        float gaitFrequency = switch (profile) {
            case BIPED -> 0.12f;
            case HEXAPOD -> 0.17f;
            case OCTOPOD -> 0.19f;
            case MYRIAPOD -> 0.24f;
            case UNKNOWN -> 0.12f;
        };

        return Optional.of(new AutoGroundContactPreset(
                profile,
                ikChains,
                groundLimbs,
                baseSwing,
                0.02f,
                gaitFrequency * speedScale,
                report
        ));
    }

    private static IkPlane planeFor(DetectedLimbChain chain) {
        float x = Math.abs(chain.anchorLocal().x());
        float z = Math.abs(chain.anchorLocal().z());

        if (x > z * 1.2f)
            return IkPlane.YZ;
        if (z > x * 1.2f)
            return IkPlane.XY;

        return IkPlane.YZ;
    }

    private static float stepDistance(LocomotionPresetType type) {
        return switch (type) {
            case BIPED -> 0.45f;
            case HEXAPOD -> 0.38f;
            case OCTOPOD -> 0.34f;
            case MYRIAPOD -> 0.24f;
            case UNKNOWN -> 0.40f;
        };
    }

    private static float stepTriggerDistance(LocomotionPresetType type) {
        return switch (type) {
            case BIPED -> 0.20f;
            case HEXAPOD -> 0.16f;
            case OCTOPOD -> 0.14f;
            case MYRIAPOD -> 0.10f;
            case UNKNOWN -> 0.20f;
        };
    }

    private static float liftHeight(LocomotionPresetType type) {
        return switch (type) {
            case BIPED -> 0.18f;
            case HEXAPOD -> 0.14f;
            case OCTOPOD -> 0.12f;
            case MYRIAPOD -> 0.08f;
            case UNKNOWN -> 0.15f;
        };
    }

    private static float probeDepth(LocomotionPresetType type) {
        return switch (type) {
            case BIPED -> 1.8f;
            case HEXAPOD -> 1.4f;
            case OCTOPOD -> 1.2f;
            case MYRIAPOD -> 1.0f;
            case UNKNOWN -> 1.5f;
        };
    }
}
