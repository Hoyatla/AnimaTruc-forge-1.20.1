package io.hoyatla.animatruc.core.graph;

import java.util.Objects;

public final class AnimationGraphTransition {
    public static final String ANY_STATE = "*";

    private final String from;
    private final String to;
    private final AnimationGraphCondition condition;
    private final float fadeTicks;
    private final int priority;

    public AnimationGraphTransition(String from, String to, AnimationGraphCondition condition, float fadeTicks, int priority) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.condition = Objects.requireNonNull(condition, "condition");
        this.fadeTicks = Math.max(0f, fadeTicks);
        this.priority = priority;
    }

    public static AnimationGraphTransition of(String from, String to, AnimationGraphCondition condition) {
        return new AnimationGraphTransition(from, to, condition, 4f, 0);
    }

    public String from() {
        return this.from;
    }

    public String to() {
        return this.to;
    }

    public AnimationGraphCondition condition() {
        return this.condition;
    }

    public float fadeTicks() {
        return this.fadeTicks;
    }

    public int priority() {
        return this.priority;
    }

    public AnimationGraphTransition withFadeTicks(float value) {
        return new AnimationGraphTransition(this.from, this.to, this.condition, value, this.priority);
    }

    public AnimationGraphTransition withPriority(int value) {
        return new AnimationGraphTransition(this.from, this.to, this.condition, this.fadeTicks, value);
    }
}
