package io.hoyatla.animatruc.core.gameplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GameplayRuntime {
    private final List<GameplayModule> modules = new ArrayList<>();
    private final GameplayRuntimeState state = new GameplayRuntimeState();
    private GameplayRuntimeConfig config;

    public GameplayRuntime(GameplayRuntimeConfig config) {
        this.config = config == null ? GameplayRuntimeConfig.DEFAULT : config;
    }

    public static GameplayRuntime standard(GameplayRuntimeConfig config) {
        GameplayRuntime runtime = new GameplayRuntime(config);
        runtime.register(new LocomotionModule());
        runtime.register(new EmoteModule());
        runtime.register(new PerceptionModule());
        runtime.register(new CombatFeedbackModule());
        runtime.register(new ExplosionFeedbackModule());
        runtime.register(new WeightModule());
        runtime.register(new InputUiModule());
        return runtime;
    }

    public void register(GameplayModule module) {
        if (module == null)
            return;
        this.modules.removeIf(existing -> existing.id().equals(module.id()));
        this.modules.add(module);
        this.modules.sort(Comparator.comparing(GameplayModule::id));
    }

    public GameplayTickResult dispatch(GameplayEvent event) {
        GameplayTickResult result = GameplayTickResult.empty();
        if (event == null || !this.config.masterEnabled())
            return result;

        for (GameplayModule module : this.modules) {
            if (!module.enabled(this.config))
                continue;
            result.merge(module.handle(event, this.state, this.config));
        }
        return result;
    }

    public ActorGameplayState actor(String actorId) {
        return this.state.actor(actorId, this.config);
    }

    public GameplayRuntimeConfig config() {
        return this.config;
    }

    public void configure(GameplayRuntimeConfig config) {
        if (config != null)
            this.config = config;
    }

    public List<GameplayModule> modules() {
        return List.copyOf(this.modules);
    }

    public GameplayRuntimeState state() {
        return this.state;
    }
}
