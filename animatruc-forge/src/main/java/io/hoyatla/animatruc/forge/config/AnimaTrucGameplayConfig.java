package io.hoyatla.animatruc.forge.config;

import io.hoyatla.animatruc.core.gameplay.GameplayRuntimeConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common gameplay toggles exposed by the AnimaTruc runtime.
 */
public final class AnimaTrucGameplayConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_RUNTIME;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LOCOMOTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EMOTES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PERCEPTION;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMBAT_FEEDBACK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXPLOSION_FEEDBACK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WEIGHT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_INPUT_UI;

    public static final ForgeConfigSpec.DoubleValue MAX_STAMINA;
    public static final ForgeConfigSpec.DoubleValue STAMINA_RECOVERY_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue STAMINA_COST_SCALE;
    public static final ForgeConfigSpec.DoubleValue PERCEPTION_RANGE;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_FEEDBACK_RANGE;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSION_RECOVERY_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_ORANGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_RED_RATIO;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_MAX_COMFORT;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("runtime");
        ENABLE_RUNTIME = BUILDER.comment("Master switch for AnimaTruc gameplay-animation runtime").define("enableRuntime", true);
        ENABLE_LOCOMOTION = BUILDER.define("enableLocomotion", true);
        ENABLE_EMOTES = BUILDER.define("enableEmotes", true);
        ENABLE_PERCEPTION = BUILDER.define("enablePerception", true);
        ENABLE_COMBAT_FEEDBACK = BUILDER.define("enableCombatFeedback", true);
        ENABLE_EXPLOSION_FEEDBACK = BUILDER.define("enableExplosionFeedback", true);
        ENABLE_WEIGHT = BUILDER.define("enableWeightFatigue", true);
        ENABLE_INPUT_UI = BUILDER.define("enableInputUi", true);
        BUILDER.pop();

        BUILDER.push("locomotion");
        MAX_STAMINA = BUILDER.comment("Runtime stamina budget used by built-in action modules").defineInRange("maxStamina", 100.0d, 1.0d, 1000.0d);
        STAMINA_RECOVERY_PER_TICK = BUILDER.defineInRange("staminaRecoveryPerTick", 0.85d, 0.0d, 20.0d);
        STAMINA_COST_SCALE = BUILDER.defineInRange("staminaCostScale", 1.0d, 0.0d, 10.0d);
        BUILDER.pop();

        BUILDER.push("perception");
        PERCEPTION_RANGE = BUILDER.defineInRange("perceptionRange", 48.0d, 1.0d, 256.0d);
        BUILDER.pop();

        BUILDER.push("feedback");
        EXPLOSION_FEEDBACK_RANGE = BUILDER.defineInRange("explosionFeedbackRange", 64.0d, 1.0d, 256.0d);
        SUPPRESSION_RECOVERY_PER_TICK = BUILDER.defineInRange("suppressionRecoveryPerTick", 0.015d, 0.0d, 1.0d);
        BUILDER.pop();

        BUILDER.push("weight");
        WEIGHT_ORANGE_RATIO = BUILDER.defineInRange("weightOrangeRatio", 0.75d, 0.01d, 20.0d);
        WEIGHT_RED_RATIO = BUILDER.defineInRange("weightRedRatio", 1.15d, 0.01d, 20.0d);
        WEIGHT_MAX_COMFORT = BUILDER.comment("Simple Forge integration comfort weight before fatigue animation starts").defineInRange("weightMaxComfort", 100.0d, 1.0d, 10000.0d);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private AnimaTrucGameplayConfig() {
    }

    public static GameplayRuntimeConfig runtimeConfig() {
        return GameplayRuntimeConfig.builder()
                .masterEnabled(ENABLE_RUNTIME.get())
                .locomotionEnabled(ENABLE_LOCOMOTION.get())
                .emotesEnabled(ENABLE_EMOTES.get())
                .perceptionEnabled(ENABLE_PERCEPTION.get())
                .combatFeedbackEnabled(ENABLE_COMBAT_FEEDBACK.get())
                .explosionFeedbackEnabled(ENABLE_EXPLOSION_FEEDBACK.get())
                .weightEnabled(ENABLE_WEIGHT.get())
                .inputUiEnabled(ENABLE_INPUT_UI.get())
                .maxStamina(MAX_STAMINA.get().floatValue())
                .staminaRecoveryPerTick(STAMINA_RECOVERY_PER_TICK.get().floatValue())
                .staminaCostScale(STAMINA_COST_SCALE.get().floatValue())
                .perceptionRange(PERCEPTION_RANGE.get().floatValue())
                .explosionFeedbackRange(EXPLOSION_FEEDBACK_RANGE.get().floatValue())
                .suppressionRecoveryPerTick(SUPPRESSION_RECOVERY_PER_TICK.get().floatValue())
                .weightOrangeRatio(WEIGHT_ORANGE_RATIO.get().floatValue())
                .weightRedRatio(WEIGHT_RED_RATIO.get().floatValue())
                .build();
    }
}
