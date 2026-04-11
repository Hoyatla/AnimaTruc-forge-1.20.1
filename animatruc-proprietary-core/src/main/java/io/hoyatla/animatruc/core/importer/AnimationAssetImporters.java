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
        String fileName = path.getFileName().toString();

        return importFromString(fileName, readPath(path), options);
    }

    public static AnimationAssetPack importFromString(String sourceName, String payload) {
        return importFromString(sourceName, payload, ModelImportOptions.DEFAULT);
    }

    public static AnimationAssetPack importFromString(String sourceName, String payload, ModelImportOptions options) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(payload, "payload");
        String fileName = sourceName.toLowerCase(Locale.ROOT);

        if (fileName.endsWith(".animatrucpack.json") || fileName.endsWith(".animatrucpack")
                || fileName.endsWith(".animatruc.json") || fileName.endsWith(".animatruc"))
            return ANIMATRUC_JSON_IMPORTER.importFromString(payload, options);
        if (fileName.endsWith(".bbmodel"))
            return BBMODEL_IMPORTER.importFromString(payload, options);
        if (fileName.endsWith(".gltf"))
            return GLTF_IMPORTER.importFromString(payload, options);

        throw new ModelImportException("Unsupported model format for source: " + sourceName);
    }

    private static String readPath(Path path) {
        try {
            return java.nio.file.Files.readString(path);
        }
        catch (java.io.IOException exception) {
            throw new ModelImportException("Failed to read source file: " + path, exception);
        }
    }
}
