package io.hoyatla.animatruc.core.skinning;

import java.util.Objects;

/**
 * Final triangle with material assignment.
 */
public final class BakedTriangle {
    private final String materialName;
    private final BakedVertex a;
    private final BakedVertex b;
    private final BakedVertex c;

    public BakedTriangle(String materialName, BakedVertex a, BakedVertex b, BakedVertex c) {
        this.materialName = materialName;
        this.a = Objects.requireNonNull(a, "a");
        this.b = Objects.requireNonNull(b, "b");
        this.c = Objects.requireNonNull(c, "c");
    }

    public String materialName() {
        return this.materialName;
    }

    public BakedVertex a() {
        return this.a;
    }

    public BakedVertex b() {
        return this.b;
    }

    public BakedVertex c() {
        return this.c;
    }
}
