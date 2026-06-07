package io.hoyatla.animatruc.core.gameplay;

import java.util.ArrayList;
import java.util.List;

public final class GameplayTickResult {
    private final List<AnimationIntent> animationIntents = new ArrayList<>();
    private final List<CameraFeedback> cameraFeedback = new ArrayList<>();
    private final List<HudSignal> hudSignals = new ArrayList<>();

    public static GameplayTickResult empty() {
        return new GameplayTickResult();
    }

    public void addAnimation(AnimationIntent intent) {
        if (intent != null)
            this.animationIntents.add(intent);
    }

    public void addCamera(CameraFeedback feedback) {
        if (feedback != null && feedback.active())
            this.cameraFeedback.add(feedback);
    }

    public void addHud(HudSignal signal) {
        if (signal != null)
            this.hudSignals.add(signal);
    }

    public void merge(GameplayTickResult other) {
        if (other == null)
            return;
        this.animationIntents.addAll(other.animationIntents);
        this.cameraFeedback.addAll(other.cameraFeedback);
        this.hudSignals.addAll(other.hudSignals);
    }

    public List<AnimationIntent> animationIntents() {
        return List.copyOf(this.animationIntents);
    }

    public List<CameraFeedback> cameraFeedback() {
        return List.copyOf(this.cameraFeedback);
    }

    public List<HudSignal> hudSignals() {
        return List.copyOf(this.hudSignals);
    }

    public CameraFeedback mergedCameraFeedback(String actorId) {
        CameraFeedback merged = new CameraFeedback(actorId, 0f, 0f, 0, 0f, 0f);
        for (CameraFeedback feedback : this.cameraFeedback)
            merged = merged.merge(feedback);
        return merged;
    }

    public boolean emptyResult() {
        return this.animationIntents.isEmpty() && this.cameraFeedback.isEmpty() && this.hudSignals.isEmpty();
    }
}
