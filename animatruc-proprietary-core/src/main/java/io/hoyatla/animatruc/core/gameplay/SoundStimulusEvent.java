package io.hoyatla.animatruc.core.gameplay;

import io.hoyatla.animatruc.core.math.Vec3f;

public record SoundStimulusEvent(String actorId, String sourceId, Vec3f position, float volume, float threat, float distance) implements GameplayEvent {
    public SoundStimulusEvent {
        if (actorId == null || actorId.isBlank())
            throw new IllegalArgumentException("actorId is required");
        sourceId = sourceId == null ? "unknown" : sourceId;
        position = position == null ? Vec3f.ZERO : position;
        volume = Math.max(0f, volume);
        threat = Math.max(0f, threat);
        distance = Math.max(0f, distance);
    }
}
