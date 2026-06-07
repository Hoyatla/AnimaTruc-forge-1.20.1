package io.hoyatla.animatruc.core.gameplay;

public record TickGameplayEvent(String actorId, float deltaTicks) implements GameplayEvent {
    public TickGameplayEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        deltaTicks = Math.max(0f, deltaTicks);
    }
}
