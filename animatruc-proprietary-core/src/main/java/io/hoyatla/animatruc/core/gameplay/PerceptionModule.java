package io.hoyatla.animatruc.core.gameplay;

public final class PerceptionModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:perception";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.PERCEPTION;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        ActorGameplayState actor = state.actor(event.actorId(), config);

        if (event instanceof SoundStimulusEvent soundEvent) {
            float distanceFactor = 1f - Math.min(1f, soundEvent.distance() / config.perceptionRange());
            float alertGain = clamp01((soundEvent.volume() * 0.55f + soundEvent.threat() * 0.45f) * distanceFactor);
            actor.addAlert(alertGain);
            actor.setLastKnownStimulus(soundEvent.position());

            if (actor.alertLevel() > 0.65f)
                result.addAnimation(AnimationIntent.override(event.actorId(), "perception/alert", actor.alertLevel(), 5, 700));
            else if (actor.alertLevel() > 0.20f)
                result.addAnimation(AnimationIntent.override(event.actorId(), "perception/investigate", actor.alertLevel(), 8, 650));

            result.addHud(new HudSignal(event.actorId(), "alert", actor.alertLevel(), 60));
        }
        else if (event instanceof TickGameplayEvent tickEvent) {
            actor.decayAlert(0.0045f * tickEvent.deltaTicks());
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
