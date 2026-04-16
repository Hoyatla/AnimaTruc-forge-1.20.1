package io.hoyatla.animatruc.forge.config;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only registration for AnimaTruc config UI extension points.
 */
public final class AnimaTrucClientBootstrap {
    private AnimaTrucClientBootstrap() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new AnimaTrucConfigMenuScreen(parent))
        );
    }
}
