package io.hoyatla.animatruc.core.gameplay;

public interface GameplayModule {
    String id();

    GameplayFeature feature();

    default boolean enabled(GameplayRuntimeConfig config) {
        return config.featureEnabled(feature());
    }

    GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config);
}
