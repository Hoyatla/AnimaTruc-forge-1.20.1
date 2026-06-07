package io.hoyatla.animatruc.core.gameplay;

public final class LocomotionModule implements GameplayModule {
    @Override
    public String id() {
        return "animatruc:locomotion";
    }

    @Override
    public GameplayFeature feature() {
        return GameplayFeature.LOCOMOTION;
    }

    @Override
    public GameplayTickResult handle(GameplayEvent event, GameplayRuntimeState state, GameplayRuntimeConfig config) {
        GameplayTickResult result = GameplayTickResult.empty();
        ActorGameplayState actor = state.actor(event.actorId(), config);

        if (event instanceof ActionRequestEvent actionEvent) {
            if (!actionEvent.pressed()) {
                actor.setActiveAction(null);
                return result;
            }

            float scaledCost = actionEvent.action().staminaCost()
                    * config.maxStamina()
                    * config.staminaCostScale()
                    * Math.max(0.2f, actionEvent.intensity());
            if (actor.stamina() < scaledCost && !actionEvent.action().sustained())
                return result;

            actor.consumeStamina(scaledCost);
            actor.setActiveAction(actionEvent.action());
            int fadeTicks = actionEvent.action().sustained() ? 4 : 2;
            result.addAnimation(AnimationIntent.override(event.actorId(), actionEvent.action().clipName(), actionEvent.intensity(), fadeTicks, 900));
            result.addHud(new HudSignal(event.actorId(), "stamina", actor.stamina() / config.maxStamina(), 40));
        }
        else if (event instanceof TickGameplayEvent tickEvent) {
            actor.recoverStamina(config.staminaRecoveryPerTick() * tickEvent.deltaTicks(), config.maxStamina());
            actor.activeAction().filter(GameplayAction::sustained).ifPresent(action -> {
                actor.consumeStamina(action.staminaCost() * config.staminaCostScale() * tickEvent.deltaTicks());
                result.addAnimation(AnimationIntent.override(event.actorId(), action.clipName(), 1f, 4, 850));
            });
        }

        return result;
    }
}
