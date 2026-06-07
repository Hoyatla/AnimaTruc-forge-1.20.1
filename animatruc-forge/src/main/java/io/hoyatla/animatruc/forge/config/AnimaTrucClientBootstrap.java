package io.hoyatla.animatruc.forge.config;

import io.hoyatla.animatruc.forge.client.AnimaTrucClientFeedback;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only registration for AnimaTruc config UI extension points.
 */
public final class AnimaTrucClientBootstrap {
    private AnimaTrucClientBootstrap() {
    }

    public static void registerConfigScreen() {
        MinecraftForge.EVENT_BUS.register(AnimaTrucClientFeedback.class);
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new AnimaTrucConfigMenuScreen(parent))
        );
    }
}
