package io.hoyatla.animatruc.core.gameplay;

import io.hoyatla.animatruc.core.math.Vec3f;

public record ExplosionStimulusEvent(String actorId, Vec3f position, float distance, float power, int affectedBlocks) implements GameplayEvent {
    public ExplosionStimulusEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        position = position == null ? Vec3f.ZERO : position;
        distance = Math.max(0f, distance);
        power = Math.max(0f, power);
        affectedBlocks = Math.max(0, affectedBlocks);
    }
}
