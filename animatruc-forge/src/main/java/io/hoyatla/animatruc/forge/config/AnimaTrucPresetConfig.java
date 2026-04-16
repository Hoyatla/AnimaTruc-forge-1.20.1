package io.hoyatla.animatruc.forge.config;

import io.hoyatla.animatruc.core.preset.LimbDetectionOptions;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client runtime configuration for automatic locomotion presets and ground contact.
 */
public final class AnimaTrucPresetConfig {
    public enum PerformanceMode {
        FULL,
        BALANCED,
        LIGHT
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_AUTO_DETECTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BIPED;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEXAPOD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OCTOPOD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MYRIAPOD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GROUND_RAYCAST;
    public static final ForgeConfigSpec.IntValue MAX_DETECTED_CHAINS;
    public static final ForgeConfigSpec.IntValue MAX_MYRIAPOD_CHAINS;
    public static final ForgeConfigSpec.DoubleValue MIN_DETECTION_CONFIDENCE;
    public static final ForgeConfigSpec.EnumValue<PerformanceMode> PERFORMANCE_MODE;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("autopreset");
        ENABLE_AUTO_DETECTION = BUILDER
                .comment("Master switch for automatic leg preset detection")
                .define("enableAutoDetection", true);
        ENABLE_BIPED = BUILDER.define("enableBipedPreset", true);
        ENABLE_HEXAPOD = BUILDER.define("enableHexapodPreset", true);
        ENABLE_OCTOPOD = BUILDER.define("enableOctopodPreset", true);
        ENABLE_MYRIAPOD = BUILDER
                .comment("Enable expensive myriapod (many legs) support")
                .define("enableMyriapodPreset", true);
        ENABLE_GROUND_RAYCAST = BUILDER
                .comment("When false, no block raycast contact is computed")
                .define("enableGroundRaycast", true);
        MIN_DETECTION_CONFIDENCE = BUILDER
                .comment("0.0 to 1.0")
                .defineInRange("minDetectionConfidence", 0.55d, 0.0d, 1.0d);
        MAX_DETECTED_CHAINS = BUILDER.defineInRange("maxDetectedChains", 16, 2, 64);
        MAX_MYRIAPOD_CHAINS = BUILDER.defineInRange("maxMyriapodChains", 24, 8, 128);
        PERFORMANCE_MODE = BUILDER.defineEnum("performanceMode", PerformanceMode.BALANCED);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private AnimaTrucPresetConfig() {
    }

    public static LimbDetectionOptions detectionOptions() {
        return LimbDetectionOptions.builder()
                .minConfidence((float)(double)MIN_DETECTION_CONFIDENCE.get())
                .maxDetectedChains(MAX_DETECTED_CHAINS.get())
                .maxMyriapodChains(MAX_MYRIAPOD_CHAINS.get())
                .allowBiped(ENABLE_BIPED.get())
                .allowHexapod(ENABLE_HEXAPOD.get())
                .allowOctopod(ENABLE_OCTOPOD.get())
                .allowMyriapod(ENABLE_MYRIAPOD.get())
                .build();
    }

    public static float speedScale() {
        return switch (PERFORMANCE_MODE.get()) {
            case FULL -> 1f;
            case BALANCED -> 0.9f;
            case LIGHT -> 0.75f;
        };
    }

    public static float liftScale() {
        return switch (PERFORMANCE_MODE.get()) {
            case FULL -> 1f;
            case BALANCED -> 0.9f;
            case LIGHT -> 0.7f;
        };
    }

    public static float smoothingScale() {
        return switch (PERFORMANCE_MODE.get()) {
            case FULL -> 1f;
            case BALANCED -> 0.85f;
            case LIGHT -> 0.65f;
        };
    }
}
