package io.hoyatla.animatruc.core.gameplay;

public final class WeightModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:weight";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.WEIGHT;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        if (!(event instanceof WeightUpdateEvent weightEvent))
            return result;

        ActorGameplayState actor = state.actor(event.actorId(), config);
        float ratio = weightEvent.ratio();
        actor.setWeightRatio(ratio);
        float display = Math.min(1f, ratio / Math.max(0.001f, config.weightRedRatio()));
        result.addHud(new HudSignal(event.actorId(), "weight", display, 80));

        if (ratio >= config.weightRedRatio())
            result.addAnimation(AnimationIntent.additive(event.actorId(), "weight/overloaded_pose", 1f, 8, 500));
        else if (ratio >= config.weightOrangeRatio())
            result.addAnimation(AnimationIntent.additive(event.actorId(), "weight/heavy_pose", display, 8, 450));

        return result;
    }
}
