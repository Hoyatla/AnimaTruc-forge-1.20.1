package io.hoyatla.animatruc.core.importer.bbmodel;

import io.hoyatla.animatruc.core.importer.ModelImportOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BbModelImporterTest {
    @Test
    void shouldImportSkeletonAndAnimationFromBbModel() {
        String bbModel = """
                {
                  "meta": { "format_version": "4.10" },
                  "outliner": [
                    {
                      "name": "body",
                      "uuid": "body_uuid",
                      "origin": [0, 24, 0],
                      "children": [
                        {
                          "name": "arm",
                          "uuid": "arm_uuid",
                          "origin": [4, 22, 0],
                          "children": []
                        }
                      ]
                    }
                  ],
                  "animations": [
                    {
                      "name": "walk",
                      "length": 1.0,
                      "loop": "loop",
                      "animators": {
                        "body_uuid": {
                          "type": "bone",
                          "name": "body",
                          "keyframes": [
                            { "channel": "rotation", "time": 0.0, "interpolation": "linear", "data_points": [{ "x": 0, "y": 0, "z": 0 }] },
                            { "channel": "rotation", "time": 1.0, "interpolation": "linear", "data_points": [{ "x": 0, "y": 45, "z": 0 }] }
                          ]
                        },
                        "arm_uuid": {
                          "type": "bone",
                          "name": "arm",
                          "keyframes": [
                            { "channel": "position", "time": 0.5, "interpolation": "step", "data_points": [{ "x": 4, "y": 0, "z": 0 }] }
                          ]
                        }
                      }
                    }
                  ]
                }
                """;

        var importer = new BbModelImporter();
        var pack = importer.importFromString(
                bbModel,
                ModelImportOptions.builder()
                        .translationScale(0.5f)
                        .ticksPerSecond(20f)
                        .build()
        );

        assertTrue(pack.skeleton().containsBone("body"));
        assertTrue(pack.skeleton().containsBone("arm"));
        assertEquals("body", pack.skeleton().bone("arm").parentName());
        assertEquals(12f, pack.skeleton().bone("body").pivot().y(), 0.0001f);

        var clip = pack.clip("walk");
        assertNotNull(clip);
        assertEquals(20f, clip.lengthTicks(), 0.0001f);
        assertTrue(clip.looping());

        var bodyAtEnd = clip.track("body").sample(19f, clip.lengthTicks(), clip.looping());
        assertNotEquals(io.hoyatla.animatruc.core.math.Quatf.IDENTITY, bodyAtEnd.rotation());

        var armAtHalf = clip.track("arm").sample(10f, clip.lengthTicks(), clip.looping());
        assertEquals(2f, armAtHalf.translation().x(), 0.0001f);
    }
}
