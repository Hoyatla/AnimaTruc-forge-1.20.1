package io.hoyatla.animatruc.core.importer.gltf;

import io.hoyatla.animatruc.core.importer.ModelImportOptions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfJsonImporterTest {
    @Test
    void shouldImportNodeAnimationFromEmbeddedGltfBuffer() {
        ByteBuffer binary = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        binary.putFloat(0f);
        binary.putFloat(1f);
        binary.putFloat(0f).putFloat(0f).putFloat(0f);
        binary.putFloat(0f).putFloat(2f).putFloat(0f);

        String base64 = Base64.getEncoder().encodeToString(binary.array());
        String gltf = """
                {
                  "asset": { "version": "2.0" },
                  "buffers": [
                    { "byteLength": 32, "uri": "data:application/octet-stream;base64,%s" }
                  ],
                  "bufferViews": [
                    { "buffer": 0, "byteOffset": 0, "byteLength": 8 },
                    { "buffer": 0, "byteOffset": 8, "byteLength": 24 }
                  ],
                  "accessors": [
                    { "bufferView": 0, "componentType": 5126, "count": 2, "type": "SCALAR" },
                    { "bufferView": 1, "componentType": 5126, "count": 2, "type": "VEC3" }
                  ],
                  "nodes": [
                    { "name": "body" }
                  ],
                  "animations": [
                    {
                      "name": "lift",
                      "samplers": [
                        { "input": 0, "output": 1, "interpolation": "LINEAR" }
                      ],
                      "channels": [
                        { "sampler": 0, "target": { "node": 0, "path": "translation" } }
                      ]
                    }
                  ]
                }
                """.formatted(base64);

        var importer = new GltfJsonImporter();
        var pack = importer.importFromString(
                gltf,
                ModelImportOptions.builder()
                        .translationScale(1f)
                        .ticksPerSecond(20f)
                        .defaultLooping(false)
                        .build()
        );

        assertTrue(pack.skeleton().containsBone("body"));
        var clip = pack.clip("lift");
        assertNotNull(clip);
        assertEquals(20f, clip.lengthTicks(), 0.0001f);
        assertFalse(clip.looping());

        var endTransform = clip.track("body").sample(20f, clip.lengthTicks(), clip.looping());
        assertEquals(2f, endTransform.translation().y(), 0.0001f);
    }
}
