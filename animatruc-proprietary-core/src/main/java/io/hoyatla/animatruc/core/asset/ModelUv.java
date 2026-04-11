package io.hoyatla.animatruc.core.asset;

/**
 * UV coordinate for one mesh face vertex.
 */
public final class ModelUv {
    private final float u;
    private final float v;

    public ModelUv(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public float u() {
        return this.u;
    }

    public float v() {
        return this.v;
    }
}
