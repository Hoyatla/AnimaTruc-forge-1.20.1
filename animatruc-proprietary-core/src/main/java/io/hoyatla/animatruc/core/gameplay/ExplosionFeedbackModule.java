package io.hoyatla.animatruc.core.gameplay;

public final class ExplosionFeedbackModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:explosion_feedback";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.EXPLOSION_FEEDBACK;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        if (!(event instanceof ExplosionStimulusEvent explosionEvent))
            return result;

        ActorGameplayState actor = state.actor(event.actorId(), config);
        float distanceFactor = 1f - Math.min(1f, explosionEvent.distance() / config.explosionFeedbackRange());
        float blockFactor = Math.min(1.5f, (float)Math.sqrt(explosionEvent.affectedBlocks() + 1) / 12f);
        float powerFactor = Math.min(1.5f, explosionEvent.power() / 6f);
        float intensity = clamp01(distanceFactor * (0.45f + blockFactor + powerFactor) * 0.65f);
        if (intensity <= 0f)
            return result;

        actor.addSuppression(intensity * 0.35f);
        int blurTicks = explosionEvent.distance() < 4f ? (int)(80 + intensity * 100) : explosionEvent.distance() < 8f ? (int)(30 + intensity * 60) : 0;
        result.addCamera(new CameraFeedback(event.actorId(), intensity, intensity * 6f, blurTicks, intensity * 0.45f, intensity * 0.25f));
        result.addAnimation(AnimationIntent.additive(event.actorId(), intensity > 0.55f ? "feedback/explosion_heavy" : "feedback/explosion_light", intensity, 3, 1250));
        result.addHud(new HudSignal(event.actorId(), "explosion", intensity, 50));
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
