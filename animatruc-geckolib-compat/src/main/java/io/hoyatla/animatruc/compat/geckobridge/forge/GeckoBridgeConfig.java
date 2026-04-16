package io.hoyatla.animatruc.compat.geckobridge.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeckoBridgeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BRIDGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_REFLECTION_LOCATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("runtime");
        ENABLE_BRIDGE = builder
                .comment("Enable the AnimaTruc GeckoLib bridge resolver.")
                .define("enableBridge", true);
        ENABLE_REFLECTION_LOCATOR = builder
                .comment("Allow reflection fallback when explicit mapping is not registered.")
                .define("enableReflectionLocator", true);
        ENABLE_DEBUG_LOGS = builder
                .comment("Emit debug logs for import/discovery events.")
                .define("enableDebugLogs", false);
        builder.pop();

        SPEC = builder.build();
    }

    private GeckoBridgeConfig() {
    }
}
