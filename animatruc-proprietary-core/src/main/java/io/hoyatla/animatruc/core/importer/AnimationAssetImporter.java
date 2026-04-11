package io.hoyatla.animatruc.core.importer;

import io.hoyatla.animatruc.core.asset.AnimationAssetPack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface AnimationAssetImporter {
    AnimationAssetPack importFromString(String payload, ModelImportOptions options);

    default AnimationAssetPack importFromPath(Path path, ModelImportOptions options) {
        Objects.requireNonNull(path, "path");

        try {
            String payload = Files.readString(path, StandardCharsets.UTF_8);
            return importFromString(payload, options);
        }
        catch (IOException exception) {
            throw new ModelImportException("Failed to read model file: " + path, exception);
        }
    }
}
