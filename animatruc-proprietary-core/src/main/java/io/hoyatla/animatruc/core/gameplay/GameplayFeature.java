package io.hoyatla.animatruc.core.gameplay;

/**
 * Functional areas exposed by the AnimaTruc gameplay-animation runtime.
 */
public enum GameplayFeature {
    LOCOMOTION("Locomotion"),
    EMOTES("Emotes"),
    PERCEPTION("Perception"),
    COMBAT_FEEDBACK("Combat Feedback"),
    EXPLOSION_FEEDBACK("Explosion Feedback"),
    WEIGHT("Weight / Fatigue"),
    INPUT_UI("Input / UI");

    private final String displayName;

    GameplayFeature(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
