package io.hoyatla.animatruc.core.gameplay;

public final class CombatFeedbackModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:combat_feedback";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.COMBAT_FEEDBACK;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        ActorGameplayState actor = state.actor(event.actorId(), config);

        if (event instanceof ProjectileNearMissEvent missEvent) {
            if (!missEvent.hostile())
                return result;

            float distanceFactor = 1f - Math.min(1f, missEvent.distance() / 8f);
            float speedFactor = Math.min(1.8f, missEvent.speed() / 2.5f);
            float impact = clamp01((missEvent.baseImpact() / 100f) * distanceFactor * Math.max(0.35f, speedFactor));
            actor.addSuppression(impact);

            result.addAnimation(AnimationIntent.additive(event.actorId(), "combat/flinch", impact, 2, 1200));
            result.addCamera(new CameraFeedback(event.actorId(), impact * 0.7f, (impact - 0.5f) * 3f, 0, actor.suppression() * 0.55f, actor.suppression() * 0.45f));
            result.addHud(new HudSignal(event.actorId(), "suppression", actor.suppression(), 70));
        }
        else if (event instanceof TickGameplayEvent tickEvent) {
            actor.decaySuppression(config.suppressionRecoveryPerTick() * tickEvent.deltaTicks());
        }

        return result;
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
