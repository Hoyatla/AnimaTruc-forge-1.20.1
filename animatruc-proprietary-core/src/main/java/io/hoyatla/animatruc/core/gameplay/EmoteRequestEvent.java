package io.hoyatla.animatruc.core.gameplay;

public record EmoteRequestEvent(String actorId, String emoteId, boolean stopCurrent, float weight) implements GameplayEvent {
    public EmoteRequestEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        emoteId = emoteId == null ? "" : emoteId.trim();
        weight = clamp01(weight);
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
