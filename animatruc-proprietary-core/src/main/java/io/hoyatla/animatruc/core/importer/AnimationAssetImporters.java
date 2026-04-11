package io.hoyatla.animatruc.core.importer;

import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.importer.animatruc.AnimaTrucJsonImporter;
import io.hoyatla.animatruc.core.importer.bbmodel.BbModelImporter;
import io.hoyatla.animatruc.core.importer.gltf.GltfJsonImporter;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Auto-selecting model importer facade for supported authoring formats.
 */
public final class AnimationAssetImporters {
    private static final AnimaTrucJsonImporter ANIMATRUC_JSON_IMPORTER = new AnimaTrucJsonImporter();
    private static final BbModelImporter BBMODEL_IMPORTER = new BbModelImporter();
    private static final GltfJsonImporter GLTF_IMPORTER = new GltfJsonImporter();

    private AnimationAssetImporters() {
    }

    public static AnimationAssetPack importFromPath(Path path) {
        return importFromPath(path, ModelImportOptions.DEFAULT);
    }

    public static AnimationAssetPack importFromPath(Path path, ModelImportOptions options) {
        Objects.requireNonNull(path, "path");
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".animatruc.json") || fileName.endsWith(".animatruc"))
            return ANIMATRUC_JSON_IMPORTER.importFromPath(path, options);
        if (fileName.endsWith(".bbmodel"))
            return BBMODEL_IMPORTER.importFromPath(path, options);
        if (fileName.endsWith(".gltf"))
            return GLTF_IMPORTER.importFromPath(path, options);

        throw new ModelImportException("Unsupported model format for file: " + path);
    }
}
