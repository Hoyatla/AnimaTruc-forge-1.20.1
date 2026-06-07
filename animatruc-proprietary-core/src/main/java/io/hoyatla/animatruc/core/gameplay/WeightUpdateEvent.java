package io.hoyatla.animatruc.core.gameplay;

public record WeightUpdateEvent(String actorId, float carriedWeight, float maxComfortWeight) implements GameplayEvent {
    public WeightUpdateEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        carriedWeight = Math.max(0f, carriedWeight);
        maxComfortWeight = Math.max(0.001f, maxComfortWeight);
    }

    public float ratio() {
        return carriedWeight / maxComfortWeight;
    }
}
