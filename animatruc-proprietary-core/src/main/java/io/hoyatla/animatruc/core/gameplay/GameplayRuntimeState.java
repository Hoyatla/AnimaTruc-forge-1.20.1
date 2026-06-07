package io.hoyatla.animatruc.core.gameplay;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class GameplayRuntimeState {
    private final Map<String, ActorGameplayState> actors = new HashMap<>();

    public ActorGameplayState actor(String actorId, GameplayRuntimeConfig config) {
        return this.actors.computeIfAbsent(actorId, key -> new ActorGameplayState(key, config));
    }

    public Collection<ActorGameplayState> actors() {
        return java.util.List.copyOf(this.actors.values());
    }

    public void clear() {
        this.actors.clear();
    }
}
