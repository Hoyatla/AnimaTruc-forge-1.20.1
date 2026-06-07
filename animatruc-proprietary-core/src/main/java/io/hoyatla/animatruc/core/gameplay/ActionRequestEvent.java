package io.hoyatla.animatruc.core.gameplay;

public record ActionRequestEvent(String actorId, GameplayAction action, boolean pressed, float intensity) implements GameplayEvent {
    public ActionRequestEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        if (action == null)
            throw new IllegalArgumentException("action is required");
        intensity = clamp01(intensity);
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
