package io.hoyatla.animatruc.core.gameplay;

public record ProjectileNearMissEvent(String actorId, float distance, float speed, float baseImpact, boolean hostile) implements GameplayEvent {
    public ProjectileNearMissEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        distance = Math.max(0f, distance);
        speed = Math.max(0f, speed);
        baseImpact = Math.max(0f, baseImpact);
    }
}
