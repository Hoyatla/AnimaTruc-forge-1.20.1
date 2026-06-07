package io.hoyatla.animatruc.core.gameplay;

/**
 * Built-in action vocabulary. Mods can still feed custom clips through the emote/runtime APIs.
 */
public enum GameplayAction {
    CRAWL("locomotion/crawl", 0.08f, true),
    DODGE_LEFT("locomotion/dodge_left", 0.28f, false),
    DODGE_RIGHT("locomotion/dodge_right", 0.28f, false),
    DODGE_BACK("locomotion/dodge_back", 0.30f, false),
    ROLL("locomotion/roll", 0.35f, false),
    SLIDE("locomotion/slide", 0.24f, false),
    VAULT("locomotion/vault", 0.32f, false),
    CLIMB_UP("locomotion/climb_up", 0.38f, false),
    WALL_RUN_LEFT("locomotion/wall_run_left", 0.20f, true),
    WALL_RUN_RIGHT("locomotion/wall_run_right", 0.20f, true),
    WALL_JUMP("locomotion/wall_jump", 0.42f, false),
    FAST_RUN("locomotion/fast_run", 0.12f, true),
    FAST_SWIM("locomotion/fast_swim", 0.12f, true),
    DIVE("locomotion/dive", 0.24f, false),
    FLIP("locomotion/flip", 0.46f, false),
    HANG("locomotion/hang", 0.06f, true),
    ZIPLINE("locomotion/zipline", 0.10f, true);

    private final String clipName;
    private final float staminaCost;
    private final boolean sustained;

    GameplayAction(String clipName, float staminaCost, boolean sustained) {
        this.clipName = clipName;
        this.staminaCost = staminaCost;
        this.sustained = sustained;
    }

    public String clipName() {
        return this.clipName;
    }

    public float staminaCost() {
        return this.staminaCost;
    }

    public boolean sustained() {
        return this.sustained;
    }
}
