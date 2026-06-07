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

    @Test
    void shouldImportMetadataMaterialsAndSmoothSkinning() {
        String payload = """
                {
                  "format": "animatruc-pack",
                  "version": 2,
                  "meta": {
                    "source": "blender",
                    "pluginId": "animatruc_blender_io",
                    "projectName": "hero",
                    "exportedAt": "2026-04-27T12:00:00Z",
                    "textures": [
                      { "name": "hero_diffuse", "path": "assets/example/textures/entity/hero.png", "width": 256, "height": 256 }
                    ],
                    "materials": [
                      { "name": "hero_material", "texture": "hero_diffuse", "renderType": "entityCutoutNoCull" }
                    ]
                  },
                  "skeleton": {
                    "bones": [
                      { "name": "root", "pivot": [0, 0, 0], "bindPose": { "translation": [0, 0, 0], "rotation": [0, 0, 0, 1], "scale": [1, 1, 1] } },
                      { "name": "arm", "parent": "root", "pivot": [2, 0, 0], "bindPose": { "translation": [0, 0, 0], "rotation": [0, 0, 0, 1], "scale": [1, 1, 1] } }
                    ]
                  },
                  "model": {
                    "meshes": [
                      {
                        "name": "hero_mesh",
                        "bone": "root",
                        "material": "hero_material",
                        "origin": [0, 0, 0],
                        "vertices": [[0, 0, 0], [2, 0, 0], [2, 1, 0]],
                        "faces": [{ "indices": [0, 1, 2], "uvs": [[0, 0], [1, 0], [1, 1]] }],
                        "skin": {
                          "modelSpaceVertices": true,
                          "influences": [
                            [{ "bone": "root", "weight": 1.0 }],
                            [{ "bone": "root", "weight": 0.25 }, { "bone": "arm", "weight": 0.75 }],
                            [{ "bone": "arm", "weight": 1.0 }]
                          ]
                        }
                      }
                    ]
                  },
                  "clips": []
                }
                """;

        AnimaTrucJsonImporter importer = new AnimaTrucJsonImporter();
        var pack = importer.importFromString(payload, ModelImportOptions.DEFAULT);

        assertEquals("hero", pack.metadata().projectName());
        assertEquals(1, pack.metadata().textures().size());
        assertEquals("hero_material", pack.metadata().materials().get(0).name());
        assertEquals(1, pack.geometry().meshes().size());
        assertEquals("hero_material", pack.geometry().meshes().get(0).materialName());
        assertTrue(pack.geometry().meshes().get(0).isSkinned());
        assertEquals(3, pack.geometry().meshes().get(0).skin().influencesByVertex().size());
        assertEquals("arm", pack.geometry().meshes().get(0).skin().influencesByVertex().get(2).get(0).boneName());
    }
}
