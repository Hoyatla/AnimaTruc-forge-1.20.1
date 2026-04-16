package io.hoyatla.animatruc.forge;

import io.hoyatla.animatruc.forge.config.AnimaTrucClientBootstrap;
import io.hoyatla.animatruc.forge.config.AnimaTrucPresetConfig;
import io.hoyatla.animatruc.forge.example.AnimaTrucExampleBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AnimaTrucForgeMod.MOD_ID)
public final class AnimaTrucForgeMod {
    public static final String MOD_ID = "animatruc";
    private static final Logger LOGGER = LogManager.getLogger();

    public AnimaTrucForgeMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AnimaTrucPresetConfig.SPEC, "animatruc-client.toml");
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> AnimaTrucClientBootstrap::registerConfigScreen);

        LOGGER.info("AnimaTruc Forge runtime initialized");
        try {
            AnimaTrucExampleBootstrap.initialize();
        }
        catch (RuntimeException exception) {
            LOGGER.error("Failed to bootstrap AnimaTruc example pack", exception);
        }
    }
}
