package io.hoyatla.animatruc.core.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationAssetImportersTest {
    @TempDir
    Path tempDirectory;

    @Test
    void shouldRouteBbModelByExtension() throws Exception {
        Path bbmodel = this.tempDirectory.resolve("test.bbmodel");
        Files.writeString(
                bbmodel,
                """
                        {
                          "outliner": [
                            { "name": "root", "uuid": "root", "origin": [0,0,0], "children": [] }
                          ],
                          "animations": []
                        }
                        """
        );

        var pack = AnimationAssetImporters.importFromPath(bbmodel);
        assertTrue(pack.skeleton().containsBone("root"));
    }

    @Test
    void shouldRouteAnimaTrucJsonByExtension() throws Exception {
        Path animatruc = this.tempDirectory.resolve("robot.animatruc.json");
        Files.writeString(
                animatruc,
                """
                        {
                          "format": "animatruc-pack",
                          "version": 1,
                          "skeleton": {
                            "bones": [
                              { "name": "root", "pivot": [0,0,0] }
                            ]
                          },
                          "clips": []
                        }
                        """
        );

        var pack = AnimationAssetImporters.importFromPath(animatruc);
        assertTrue(pack.skeleton().containsBone("root"));
    }

    @Test
    void shouldRouteAnimaTrucPackByExtension() throws Exception {
        Path animatrucPack = this.tempDirectory.resolve("robot.animatrucpack.json");
        Files.writeString(
                animatrucPack,
                """
                        {
                          "format": "animatruc-pack",
                          "version": 2,
                          "skeleton": {
                            "bones": [
                              { "name": "root", "pivot": [0,0,0] }
                            ]
                          },
                          "model": {
                            "cubes": [
                              { "name": "cube", "bone": "root", "from": [0,0,0], "to": [1,1,1] }
                            ]
                          },
                          "clips": []
                        }
                        """
        );

        var pack = AnimationAssetImporters.importFromPath(animatrucPack);
        assertTrue(pack.skeleton().containsBone("root"));
        assertTrue(!pack.geometry().isEmpty());
    }

    @Test
    void shouldRejectUnsupportedExtension() throws Exception {
        Path unsupported = this.tempDirectory.resolve("test.json");
        Files.writeString(unsupported, "{}");

        assertThrows(ModelImportException.class, () -> AnimationAssetImporters.importFromPath(unsupported));
    }
}
