package io.hoyatla.animatruc.core.asset;

import java.util.Objects;

/**
 * Optional material metadata exported by authoring tools.
 */
public final class PackMaterial {
    private final String name;
    private final String textureName;
    private final String renderType;

    public PackMaterial(String name, String textureName, String renderType) {
        this.name = Objects.requireNonNull(name, "name");
        this.textureName = textureName;
        this.renderType = renderType;
    }

    public String name() {
        return this.name;
    }

    public String textureName() {
        return this.textureName;
    }

    public String renderType() {
        return this.renderType;
    }
}
