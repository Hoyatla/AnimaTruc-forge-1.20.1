package io.hoyatla.animatruc.core.gameplay;

public record CameraFeedback(String actorId, float shake, float rollDegrees, int blurTicks, float vignette, float soundMuffle) {
    public CameraFeedback {
        actorId = actorId == null || actorId.isBlank() ? "unknown" : actorId;
        shake = clamp01(shake);
        blurTicks = Math.max(0, blurTicks);
        vignette = clamp01(vignette);
        soundMuffle = clamp01(soundMuffle);
    }

    public boolean active() {
        return this.shake > 0f || this.rollDegrees != 0f || this.blurTicks > 0 || this.vignette > 0f || this.soundMuffle > 0f;
    }

    public CameraFeedback merge(CameraFeedback other) {
        if (other == null || !other.active())
            return this;
        if (!this.active())
            return other;

        return new CameraFeedback(
                this.actorId,
                Math.max(this.shake, other.shake),
                this.rollDegrees + other.rollDegrees,
                Math.max(this.blurTicks, other.blurTicks),
                Math.max(this.vignette, other.vignette),
                Math.max(this.soundMuffle, other.soundMuffle)
        );
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;
        return value;
    }
}
