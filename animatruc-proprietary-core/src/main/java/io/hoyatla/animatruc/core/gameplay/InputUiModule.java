package io.hoyatla.animatruc.core.gameplay;

import java.util.Locale;
import java.util.Set;

public final class InputUiModule implements GameplayModule {
    private static final Set<String> DEFAULT_BLOCKED_SCREEN_TOKENS = Set.of("chat", "pause", "options", "title", "death");

    @Override
    public String id() {
        return "animatruc:input_ui";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.INPUT_UI;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        return GameplayTickResult.empty();
    }

    public boolean allowMovementInScreen(String screenClassName, GameplayRuntimeConfig config) {
        if (!enabled(config))
            return false;
        if (screenClassName == null || screenClassName.isBlank())
            return true;

        String normalized = screenClassName.toLowerCase(Locale.ROOT);
        for (String token : DEFAULT_BLOCKED_SCREEN_TOKENS) {
            if (normalized.contains(token))
                return false;
        }
        return true;
    }
}
