package io.hoyatla.animatruc.forge.preset;

import io.hoyatla.animatruc.core.modifier.IkChainDefinition;
import io.hoyatla.animatruc.core.modifier.MultiLimbIKModifier;
import io.hoyatla.animatruc.core.preset.LimbDetectionReport;
import io.hoyatla.animatruc.core.preset.LocomotionPresetType;
import io.hoyatla.animatruc.forge.ground.GroundContactController;
import io.hoyatla.animatruc.forge.ground.GroundContactLimbDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Fully prepared runtime preset bundle:
 * - IK chain modifier setup
 * - Ground-contact stepping controller
 * - Detection metadata for diagnostics
 */
public final class AutoGroundContactPreset {
    private final LocomotionPresetType profile;
    private final List<IkChainDefinition> ikChains;
    private final List<GroundContactLimbDefinition> groundLimbs;
    private final float baseSwingDurationTicks;
    private final float minHorizontalSpeed;
    private final float gaitFrequency;
    private final LimbDetectionReport detectionReport;

    public AutoGroundContactPreset(
            LocomotionPresetType profile,
            List<IkChainDefinition> ikChains,
            List<GroundContactLimbDefinition> groundLimbs,
            float baseSwingDurationTicks,
            float minHorizontalSpeed,
            float gaitFrequency,
            LimbDetectionReport detectionReport) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.ikChains = List.copyOf(Objects.requireNonNull(ikChains, "ikChains"));
        this.groundLimbs = List.copyOf(Objects.requireNonNull(groundLimbs, "groundLimbs"));
        this.baseSwingDurationTicks = Math.max(0.1f, baseSwingDurationTicks);
        this.minHorizontalSpeed = Math.max(0f, minHorizontalSpeed);
        this.gaitFrequency = Math.max(0.001f, gaitFrequency);
        this.detectionReport = Objects.requireNonNull(detectionReport, "detectionReport");
    }

    public LocomotionPresetType profile() {
        return this.profile;
    }

    public List<IkChainDefinition> ikChains() {
        return this.ikChains;
    }

    public List<GroundContactLimbDefinition> groundLimbs() {
        return this.groundLimbs;
    }

    public float baseSwingDurationTicks() {
        return this.baseSwingDurationTicks;
    }

    public float minHorizontalSpeed() {
        return this.minHorizontalSpeed;
    }

    public float gaitFrequency() {
        return this.gaitFrequency;
    }

    public LimbDetectionReport detectionReport() {
        return this.detectionReport;
    }

    public MultiLimbIKModifier createIkModifier() {
        return new MultiLimbIKModifier(this.ikChains, false);
    }

    public GroundContactController createGroundController() {
        return new GroundContactController(
                this.groundLimbs,
                this.baseSwingDurationTicks,
                this.minHorizontalSpeed,
                this.gaitFrequency
        );
    }
}
