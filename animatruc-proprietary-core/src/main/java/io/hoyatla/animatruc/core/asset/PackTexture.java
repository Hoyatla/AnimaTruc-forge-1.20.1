package io.hoyatla.animatruc.core.asset;

import java.util.Objects;

/**
 * Optional texture metadata exported by authoring tools.
 */
public final class PackTexture {
    private final String name;
    private final String path;
    private final int width;
    private final int height;

    public PackTexture(String name, String path, int width, int height) {
        this.name = Objects.requireNonNull(name, "name");
        this.path = path;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public String name() {
        return this.name;
    }

    public String path() {
        return this.path;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
