package io.hoyatla.animatruc.core.gameplay;

public record AnimationIntent(String actorId, String clipName, float weight, int fadeTicks, int priority, boolean additive) {
    public AnimationIntent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        if (clipName == null || clipName.isBlank())
            throw new IllegalArgumentException("clipName is required");
        weight = clamp01(weight);
        fadeTicks = Math.max(0, fadeTicks);
    }

    public static AnimationIntent override(String actorId, String clipName, float weight, int fadeTicks, int priority) {
        return new AnimationIntent(actorId, clipName, weight, fadeTicks, priority, false);
    }

    public static AnimationIntent additive(String actorId, String clipName, float weight, int fadeTicks, int priority) {
        return new AnimationIntent(actorId, clipName, weight, fadeTicks, priority, true);
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
