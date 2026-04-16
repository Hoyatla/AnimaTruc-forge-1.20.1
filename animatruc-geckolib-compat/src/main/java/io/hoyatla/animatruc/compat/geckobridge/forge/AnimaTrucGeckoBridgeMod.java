package io.hoyatla.animatruc.compat.geckobridge.forge;

import io.hoyatla.animatruc.compat.geckobridge.bridge.GeckoBridgeApi;
import io.hoyatla.animatruc.compat.geckobridge.bridge.GeckoBridgeRuntime;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AnimaTrucGeckoBridgeMod.MOD_ID)
public final class AnimaTrucGeckoBridgeMod {
    public static final String MOD_ID = "animatruc_geckobridge";
    private static final Logger LOGGER = LogManager.getLogger();

    private final GeckoBridgeRuntime runtime = new GeckoBridgeRuntime();

    public AnimaTrucGeckoBridgeMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeckoBridgeConfig.SPEC, "animatruc-geckobridge-common.toml");
        GeckoBridgeApi.bootstrap(this.runtime);
        this.runtime.registerResolver();
        MinecraftForge.EVENT_BUS.addListener(this::onReloadListeners);

        LOGGER.info("AnimaTruc GeckoLib Bridge initialized. GeckoLib remains an external required dependency.");
    }

    private void onReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new GeckoBridgeReloadListener(this.runtime.cache()));
    }
}
