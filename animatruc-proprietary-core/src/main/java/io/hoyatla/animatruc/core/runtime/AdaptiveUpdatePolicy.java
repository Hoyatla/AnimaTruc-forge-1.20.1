package io.hoyatla.animatruc.core.runtime;

/**
 * Tick throttling profile designed for large entity counts.
 */
public final class AdaptiveUpdatePolicy {
    public static final AdaptiveUpdatePolicy DEFAULT = new Builder().build();

    private final float nearDistance;
    private final float coarseDistance;
    private final float frozenDistance;
    private final int nearIntervalTicks;
    private final int coarseIntervalTicks;
    private final int farIntervalTicks;
    private final int frozenIntervalTicks;
    private final int invisibleIntervalTicks;

    private AdaptiveUpdatePolicy(Builder builder) {
        this.nearDistance = builder.nearDistance;
        this.coarseDistance = builder.coarseDistance;
        this.frozenDistance = builder.frozenDistance;
        this.nearIntervalTicks = builder.nearIntervalTicks;
        this.coarseIntervalTicks = builder.coarseIntervalTicks;
        this.farIntervalTicks = builder.farIntervalTicks;
        this.frozenIntervalTicks = builder.frozenIntervalTicks;
        this.invisibleIntervalTicks = builder.invisibleIntervalTicks;
    }

    public int intervalFor(float distanceToCamera, boolean visible, boolean forceTick) {
        if (forceTick)
            return 1;
        if (!visible)
            return this.invisibleIntervalTicks;
        if (distanceToCamera <= this.nearDistance)
            return this.nearIntervalTicks;
        if (distanceToCamera <= this.coarseDistance)
            return this.coarseIntervalTicks;
        if (distanceToCamera <= this.frozenDistance)
            return this.farIntervalTicks;

        return this.frozenIntervalTicks;
    }

    public static final class Builder {
        private float nearDistance = 20f;
        private float coarseDistance = 48f;
        private float frozenDistance = 96f;
        private int nearIntervalTicks = 1;
        private int coarseIntervalTicks = 2;
        private int farIntervalTicks = 4;
        private int frozenIntervalTicks = 10;
        private int invisibleIntervalTicks = 6;

        public Builder nearDistance(float value) {
            this.nearDistance = Math.max(0f, value);
            return this;
        }

        public Builder coarseDistance(float value) {
            this.coarseDistance = Math.max(0f, value);
            return this;
        }

        public Builder frozenDistance(float value) {
            this.frozenDistance = Math.max(0f, value);
            return this;
        }

        public Builder nearIntervalTicks(int value) {
            this.nearIntervalTicks = Math.max(1, value);
            return this;
        }

        public Builder coarseIntervalTicks(int value) {
            this.coarseIntervalTicks = Math.max(1, value);
            return this;
        }

        public Builder farIntervalTicks(int value) {
            this.farIntervalTicks = Math.max(1, value);
            return this;
        }

        public Builder frozenIntervalTicks(int value) {
            this.frozenIntervalTicks = Math.max(1, value);
            return this;
        }

        public Builder invisibleIntervalTicks(int value) {
            this.invisibleIntervalTicks = Math.max(1, value);
            return this;
        }

        public AdaptiveUpdatePolicy build() {
            return new AdaptiveUpdatePolicy(this);
        }
    }
}
