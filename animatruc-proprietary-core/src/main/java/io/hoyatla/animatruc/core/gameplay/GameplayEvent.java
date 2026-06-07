package io.hoyatla.animatruc.core.gameplay;

/**
 * Base type for gameplay events that can drive animation, HUD, camera and AI feedback.
 */
public sealed interface GameplayEvent permits ActionRequestEvent, EmoteRequestEvent, ExplosionStimulusEvent,
        ProjectileNearMissEvent, SoundStimulusEvent, TickGameplayEvent, WeightUpdateEvent {
    String actorId();
}
