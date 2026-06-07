package io.hoyatla.animatruc.core.gameplay;

public final class GameplayRuntimeFactory {
    private GameplayRuntimeFactory() {
    }

    public static GameplayRuntime createStandardRuntime() {
        return GameplayRuntime.standard(GameplayRuntimeConfig.DEFAULT);
    }

    public static GameplayRuntime createStandardRuntime(GameplayRuntimeConfig config) {
        return GameplayRuntime.standard(config == null ? GameplayRuntimeConfig.DEFAULT : config);
    }
}
