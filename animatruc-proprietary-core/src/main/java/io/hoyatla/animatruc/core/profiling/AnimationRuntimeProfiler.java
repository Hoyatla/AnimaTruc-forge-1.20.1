package io.hoyatla.animatruc.core.profiling;

/**
 * Lightweight runtime profiler for animation frame phases.
 */
public final class AnimationRuntimeProfiler {
    private boolean enabled;
    private long frames;
    private long skippedFrames;
    private long graphNanos;
    private long mixNanos;
    private long modifierNanos;
    private long totalNanos;
    private long peakFrameNanos;

    public boolean enabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void record(long graphNanos, long mixNanos, long modifierNanos, long totalNanos, boolean advancedFrame) {
        if (!this.enabled)
            return;

        if (!advancedFrame) {
            this.skippedFrames++;
            return;
        }

        this.frames++;
        this.graphNanos += Math.max(0L, graphNanos);
        this.mixNanos += Math.max(0L, mixNanos);
        this.modifierNanos += Math.max(0L, modifierNanos);
        this.totalNanos += Math.max(0L, totalNanos);
        this.peakFrameNanos = Math.max(this.peakFrameNanos, totalNanos);
    }

    public Snapshot snapshot() {
        long safeFrames = Math.max(1L, this.frames);

        return new Snapshot(
                this.enabled,
                this.frames,
                this.skippedFrames,
                this.graphNanos / safeFrames,
                this.mixNanos / safeFrames,
                this.modifierNanos / safeFrames,
                this.totalNanos / safeFrames,
                this.peakFrameNanos
        );
    }

    public void reset() {
        this.frames = 0L;
        this.skippedFrames = 0L;
        this.graphNanos = 0L;
        this.mixNanos = 0L;
        this.modifierNanos = 0L;
        this.totalNanos = 0L;
        this.peakFrameNanos = 0L;
    }

    public record Snapshot(
            boolean enabled,
            long frames,
            long skippedFrames,
            long averageGraphNanos,
            long averageMixNanos,
            long averageModifierNanos,
            long averageTotalNanos,
            long peakFrameNanos) {
    }
}
