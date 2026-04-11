package io.hoyatla.animatruc.core.e2e;

import io.hoyatla.animatruc.core.importer.AnimationAssetImporters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginExportRuntimeE2ETest {
    @TempDir
    Path tempDirectory;

    @Test
    void shouldImportPluginExportedRuntimePack() throws Exception {
        var stream = this.getClass().getClassLoader().getResourceAsStream("e2e/plugin_export_humanoid.animatrucpack.json");
        assertNotNull(stream);
        String payload = new String(stream.readAllBytes());

        Path runtimePack = this.tempDirectory.resolve("plugin_export_humanoid.animatrucpack.json");
        Files.writeString(runtimePack, payload);

        var pack = AnimationAssetImporters.importFromPath(runtimePack);

        assertTrue(pack.skeleton().containsBone("body"));
        assertTrue(pack.skeleton().containsBone("head"));
        assertEquals(2, pack.geometry().cubes().size());

        var clip = pack.clip("idle");
        assertNotNull(clip);
        assertEquals(20f, clip.lengthTicks(), 0.0001f);
        assertTrue(clip.looping());
        assertNotNull(clip.track("head"));
    }
}
