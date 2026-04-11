package io.hoyatla.animatruc.forge.pack;

import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.importer.AnimationAssetImporters;
import io.hoyatla.animatruc.core.importer.ModelImportException;
import io.hoyatla.animatruc.core.importer.ModelImportOptions;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Forge-side loader for unified AnimaTruc runtime packs.
 */
public final class AnimaTrucForgePackLoader {
    public AnimationAssetPack loadFromFile(Path file) {
        return AnimationAssetImporters.importFromPath(file, ModelImportOptions.DEFAULT);
    }

    public AnimationAssetPack loadFromModResources(ResourceLocation location) {
        return loadFromModResources(location, ModelImportOptions.DEFAULT);
    }

    public AnimationAssetPack loadFromModResources(ResourceLocation location, ModelImportOptions options) {
        Objects.requireNonNull(location, "location");
        String classpathLocation = "assets/" + location.getNamespace() + "/animatrucpacks/" + location.getPath();

        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            if (stream == null)
                throw new ModelImportException("Missing AnimaTruc pack resource: " + classpathLocation);

            String payload = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return AnimationAssetImporters.importFromString(classpathLocation, payload, options);
        }
        catch (IOException exception) {
            throw new ModelImportException("Failed to read AnimaTruc pack resource: " + classpathLocation, exception);
        }
    }
}
