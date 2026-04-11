package io.hoyatla.animatruc.forge;

import io.hoyatla.animatruc.forge.example.AnimaTrucExampleBootstrap;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AnimaTrucForgeMod.MOD_ID)
public final class AnimaTrucForgeMod {
    public static final String MOD_ID = "animatruc";
    private static final Logger LOGGER = LogManager.getLogger();

    public AnimaTrucForgeMod() {
        LOGGER.info("AnimaTruc Forge runtime initialized");
        try {
            AnimaTrucExampleBootstrap.initialize();
        }
        catch (RuntimeException exception) {
            LOGGER.error("Failed to bootstrap AnimaTruc example pack", exception);
        }
    }
}
