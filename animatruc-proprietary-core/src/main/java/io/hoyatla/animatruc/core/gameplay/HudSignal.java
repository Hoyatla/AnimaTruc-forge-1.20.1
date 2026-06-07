package io.hoyatla.animatruc.core.gameplay;

public record HudSignal(String actorId, String channel, float value, int visibleTicks) {
    public HudSignal {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        if (channel == null || channel.isBlank())
            throw new IllegalArgumentException("channel is required");
        value = clamp01(value);
        visibleTicks = Math.max(0, visibleTicks);
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
