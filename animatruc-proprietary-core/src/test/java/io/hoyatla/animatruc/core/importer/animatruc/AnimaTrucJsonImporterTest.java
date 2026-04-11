package io.hoyatla.animatruc.core.importer.animatruc;

import io.hoyatla.animatruc.core.importer.ModelImportOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimaTrucJsonImporterTest {
    @Test
    void shouldImportAnimaTrucPackWithQuaternionAndEulerRotation() {
        String payload = """
                {
                  "format": "animatruc-pack",
                  "version": 1,
                  "skeleton": {
                    "bones": [
                      {
                        "name": "body",
                        "pivot": [0, 24, 0],
                        "bindPose": {
                          "translation": [0, 0, 0],
                          "rotation": [0, 0, 0, 1],
                          "scale": [1, 1, 1]
                        }
                      },
                      {
                        "name": "head",
                        "parent": "body",
                        "pivot": [0, 30, 0],
                        "bindPose": {
                          "translation": [0, 0, 0],
                          "rotation": [0, 0, 0],
                          "scale": [1, 1, 1]
                        }
                      }
                    ]
                  },
                  "model": {
                    "cubes": [
                      {
                        "name": "body_cube",
                        "bone": "body",
                        "from": [-4, 12, -2],
                        "to": [4, 24, 2],
                        "inflate": 0,
                        "mirror": false
                      }
                    ],
                    "meshes": [
                      {
                        "name": "head_mesh",
                        "bone": "head",
                        "origin": [0, 30, 0],
                        "vertices": [
                          [0, 0, 0],
                          [1, 0, 0],
                          [0, 1, 0]
                        ],
                        "faces": [
                          {
                            "indices": [0, 1, 2],
                            "uvs": [[0, 0], [1, 0], [0, 1]]
                          }
                        ]
                      }
                    ]
                  },
                  "clips": [
                    {
                      "name": "idle",
                      "lengthTicks": 20,
                      "looping": true,
                      "tracks": {
                        "head": {
                          "rotation": [
                            { "tick": 0, "interpolation": "LINEAR", "value": [0, 0, 0, 1] },
                            { "tick": 20, "interpolation": "LINEAR", "value": [0, 45, 0] }
                          ]
                        }
                      }
                    }
                  ]
                }
                """;

        AnimaTrucJsonImporter importer = new AnimaTrucJsonImporter();
        var pack = importer.importFromString(
                payload,
                ModelImportOptions.builder()
                        .translationScale(0.5f)
                        .build()
        );

        assertTrue(pack.skeleton().containsBone("body"));
        assertEquals("body", pack.skeleton().bone("head").parentName());
        assertEquals(12f, pack.skeleton().bone("body").pivot().y(), 0.0001f);
        assertEquals(1, pack.geometry().cubes().size());
        assertEquals("body", pack.geometry().cubes().get(0).boneName());
        assertEquals(1, pack.geometry().meshes().size());
        assertEquals("head", pack.geometry().meshes().get(0).boneName());
        assertEquals(3, pack.geometry().meshes().get(0).vertices().size());
        assertEquals(1, pack.geometry().meshes().get(0).faces().size());

        var clip = pack.clip("idle");
        assertNotNull(clip);
        assertTrue(clip.looping());
        assertEquals(20f, clip.lengthTicks(), 0.0001f);

        var headTransform = clip.track("head").sample(19f, clip.lengthTicks(), clip.looping());
        assertNotNull(headTransform.rotation());
    }
}
