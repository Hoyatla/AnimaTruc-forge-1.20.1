package io.hoyatla.animatruc.forge.gameplay;

import io.hoyatla.animatruc.core.gameplay.GameplayEvent;
import io.hoyatla.animatruc.core.gameplay.GameplayRuntime;
import io.hoyatla.animatruc.core.gameplay.GameplayTickResult;
import io.hoyatla.animatruc.core.runtime.AnimaTrucRuntime;
import io.hoyatla.animatruc.forge.config.AnimaTrucGameplayConfig;

public final class AnimaTrucForgeGameplayRuntime {
    private static final GameplayRuntime RUNTIME = AnimaTrucRuntime.createGameplayRuntime(AnimaTrucGameplayConfig.runtimeConfig());

    private AnimaTrucForgeGameplayRuntime() {
    }

    public static GameplayRuntime runtime() {
        RUNTIME.configure(AnimaTrucGameplayConfig.runtimeConfig());
        return RUNTIME;
    }

    public static GameplayTickResult dispatch(GameplayEvent event) {
        return runtime().dispatch(event);
    }
}
