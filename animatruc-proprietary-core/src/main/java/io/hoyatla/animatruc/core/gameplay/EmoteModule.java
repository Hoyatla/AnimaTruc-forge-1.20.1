package io.hoyatla.animatruc.core.gameplay;

public final class EmoteModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:emotes";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.EMOTES;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        if (!(event instanceof EmoteRequestEvent emoteEvent))
            return result;

        ActorGameplayState actor = state.actor(event.actorId(), config);
        if (emoteEvent.stopCurrent() || emoteEvent.emoteId().isBlank()) {
            actor.setActiveEmote("");
            result.addAnimation(AnimationIntent.override(event.actorId(), "emote/stop", 1f, 6, 1000));
            return result;
        }

        actor.setActiveEmote(emoteEvent.emoteId());
        result.addAnimation(AnimationIntent.override(event.actorId(), "emote/" + emoteEvent.emoteId(), emoteEvent.weight(), 6, 1100));
        result.addHud(new HudSignal(event.actorId(), "emote", 1f, 40));
        return result;
    }
}
