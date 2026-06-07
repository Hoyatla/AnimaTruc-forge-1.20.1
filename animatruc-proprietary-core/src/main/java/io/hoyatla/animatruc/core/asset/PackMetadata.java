package io.hoyatla.animatruc.core.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Optional pack metadata that tools may attach for rendering and diagnostics.
 */
public final class PackMetadata {
    public static final PackMetadata EMPTY = new PackMetadata(null, null, null, null, List.of(), List.of());

    private final String source;
    private final String pluginId;
    private final String projectName;
    private final String exportedAt;
    private final List<PackTexture> textures;
    private final List<PackMaterial> materials;

    public PackMetadata(
            String source,
            String pluginId,
            String projectName,
            String exportedAt,
            List<PackTexture> textures,
            List<PackMaterial> materials) {
        this.source = source;
        this.pluginId = pluginId;
        this.projectName = projectName;
        this.exportedAt = exportedAt;
        this.textures = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(textures, "textures")));
        this.materials = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(materials, "materials")));
    }

    public String source() {
        return this.source;
    }

    public String pluginId() {
        return this.pluginId;
    }

    public String projectName() {
        return this.projectName;
    }

    public String exportedAt() {
        return this.exportedAt;
    }

    public List<PackTexture> textures() {
        return this.textures;
    }

    public List<PackMaterial> materials() {
        return this.materials;
    }
}
