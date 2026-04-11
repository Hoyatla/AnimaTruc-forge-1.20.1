package io.hoyatla.animatruc.core.importer.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.hoyatla.animatruc.core.animation.AnimationChannel;
import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.animation.BoneAnimationTrack;
import io.hoyatla.animatruc.core.animation.InterpolationMode;
import io.hoyatla.animatruc.core.animation.Interpolators;
import io.hoyatla.animatruc.core.animation.Keyframe;
import io.hoyatla.animatruc.core.animation.Transform;
import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.asset.ModelBone;
import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.importer.AnimationAssetImporter;
import io.hoyatla.animatruc.core.importer.ModelImportException;
import io.hoyatla.animatruc.core.importer.ModelImportOptions;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Imports glTF JSON (.gltf) nodes and animation channels into AnimaTruc clips.
 * Supported accessors: FLOAT / SCALAR / VEC3 / VEC4.
 */
public final class GltfJsonImporter implements AnimationAssetImporter {
    @Override
    public AnimationAssetPack importFromString(String payload, ModelImportOptions options) {
        return importInternal(payload, null, options);
    }

    @Override
    public AnimationAssetPack importFromPath(Path path, ModelImportOptions options) {
        Objects.requireNonNull(path, "path");

        try {
            String payload = Files.readString(path, StandardCharsets.UTF_8);
            return importInternal(payload, path.getParent(), options);
        }
        catch (IOException exception) {
            throw new ModelImportException("Failed to read glTF file: " + path, exception);
        }
    }

    private static AnimationAssetPack importInternal(String payload, Path baseDirectory, ModelImportOptions options) {
        Objects.requireNonNull(payload, "payload");
        ModelImportOptions safeOptions = options == null ? ModelImportOptions.DEFAULT : options;

        JsonObject root = parseRoot(payload);
        DocumentContext context = new DocumentContext(root, baseDirectory);
        List<NodeInfo> nodes = parseNodes(root, safeOptions.translationScale());
        ModelSkeleton skeleton = buildSkeleton(nodes);
        Map<String, AnimationClip> clips = parseAnimations(root, context, nodes, safeOptions);

        return new AnimationAssetPack(skeleton, clips);
    }

    private static JsonObject parseRoot(String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);

            if (!element.isJsonObject())
                throw new ModelImportException("Invalid glTF payload: root must be an object.");

            return element.getAsJsonObject();
        }
        catch (JsonParseException exception) {
            throw new ModelImportException("Invalid glTF JSON.", exception);
        }
    }

    private static List<NodeInfo> parseNodes(JsonObject root, float translationScale) {
        JsonArray nodesArray = getArray(root, "nodes");
        List<NodeInfo> nodes = new ArrayList<>();

        if (nodesArray == null)
            return nodes;

        for (int index = 0; index < nodesArray.size(); index++) {
            JsonObject nodeObject = asObject(nodesArray.get(index));

            if (nodeObject == null) {
                nodes.add(new NodeInfo("node_" + index, -1, Vec3f.ZERO, Transform.IDENTITY));
                continue;
            }

            String name = getString(nodeObject, "name", "node_" + index);
            Vec3f translation = parseVector(getArray(nodeObject, "translation"), Vec3f.ZERO, translationScale);
            Vec3f scale = parseVector(getArray(nodeObject, "scale"), Vec3f.ONE, 1f);
            Quatf rotation = parseQuaternion(getArray(nodeObject, "rotation"), Quatf.IDENTITY);
            Transform bindTransform = new Transform(translation, rotation, scale);

            nodes.add(new NodeInfo(name, -1, translation, bindTransform));
        }

        for (int parentIndex = 0; parentIndex < nodesArray.size(); parentIndex++) {
            JsonObject parentNode = asObject(nodesArray.get(parentIndex));

            if (parentNode == null)
                continue;

            JsonArray children = getArray(parentNode, "children");

            if (children == null)
                continue;

            for (JsonElement childElement : children) {
                int childIndex = parseInt(childElement, -1);

                if (childIndex < 0 || childIndex >= nodes.size())
                    continue;

                NodeInfo child = nodes.get(childIndex);
                nodes.set(childIndex, new NodeInfo(child.name, parentIndex, child.pivot, child.bindTransform));
            }
        }

        return nodes;
    }

    private static ModelSkeleton buildSkeleton(List<NodeInfo> nodes) {
        List<ModelBone> bones = new ArrayList<>(nodes.size());

        for (int index = 0; index < nodes.size(); index++) {
            NodeInfo node = nodes.get(index);
            String parentName = node.parentIndex >= 0 && node.parentIndex < nodes.size()
                    ? nodes.get(node.parentIndex).name
                    : null;

            bones.add(new ModelBone(node.name, parentName, node.pivot, node.bindTransform));
        }

        return new ModelSkeleton(bones);
    }

    private static Map<String, AnimationClip> parseAnimations(
            JsonObject root,
            DocumentContext context,
            List<NodeInfo> nodes,
            ModelImportOptions options) {
        Map<String, AnimationClip> clipsByName = new LinkedHashMap<>();
        JsonArray animationsArray = getArray(root, "animations");

        if (animationsArray == null)
            return clipsByName;

        for (int animationIndex = 0; animationIndex < animationsArray.size(); animationIndex++) {
            JsonObject animationObject = asObject(animationsArray.get(animationIndex));

            if (animationObject == null)
                continue;

            String clipName = uniqueClipName(
                    clipsByName,
                    getString(animationObject, "name", "clip_" + animationIndex)
            );
            JsonArray samplers = getArray(animationObject, "samplers");
            JsonArray channels = getArray(animationObject, "channels");

            if (samplers == null || channels == null) {
                clipsByName.put(clipName, new AnimationClip(clipName, 0f, options.defaultLooping(), Map.of()));
                continue;
            }

            Map<String, TrackBuilder> tracks = new LinkedHashMap<>();
            float maxTick = 0f;

            for (JsonElement channelElement : channels) {
                JsonObject channelObject = asObject(channelElement);

                if (channelObject == null)
                    continue;

                int samplerIndex = parseInt(channelObject.get("sampler"), -1);

                if (samplerIndex < 0 || samplerIndex >= samplers.size())
                    continue;

                JsonObject samplerObject = asObject(samplers.get(samplerIndex));

                if (samplerObject == null)
                    continue;

                JsonObject targetObject = getObject(channelObject, "target");

                if (targetObject == null)
                    continue;

                int nodeIndex = parseInt(targetObject.get("node"), -1);

                if (nodeIndex < 0 || nodeIndex >= nodes.size())
                    continue;

                String targetPath = normalizeTargetPath(getString(targetObject, "path", ""));

                if (targetPath == null)
                    continue;

                int inputAccessorIndex = parseInt(samplerObject.get("input"), -1);
                int outputAccessorIndex = parseInt(samplerObject.get("output"), -1);

                if (inputAccessorIndex < 0 || outputAccessorIndex < 0)
                    continue;

                float[] times = context.readAccessorFloats(inputAccessorIndex);
                float[] values = context.readAccessorFloats(outputAccessorIndex);
                int outputArity = context.accessorArity(outputAccessorIndex);
                InterpolationMode interpolationMode = parseInterpolationMode(getString(samplerObject, "interpolation", "LINEAR"));

                if (times.length == 0 || outputArity <= 0)
                    continue;

                int frameCount = Math.min(times.length, values.length / outputArity);
                String boneName = nodes.get(nodeIndex).name;
                TrackBuilder builder = tracks.computeIfAbsent(boneName, unused -> new TrackBuilder());

                for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                    float tick = Math.max(0f, times[frameIndex] * options.ticksPerSecond());
                    maxTick = Math.max(maxTick, tick);
                    int base = frameIndex * outputArity;

                    if ("translation".equals(targetPath) && outputArity >= 3) {
                        builder.translation.add(
                                new Keyframe<>(
                                        tick,
                                        new Vec3f(
                                                values[base] * options.translationScale(),
                                                values[base + 1] * options.translationScale(),
                                                values[base + 2] * options.translationScale()
                                        ),
                                        interpolationMode
                                )
                        );
                    }
                    else if ("rotation".equals(targetPath) && outputArity >= 4) {
                        builder.rotation.add(
                                new Keyframe<>(
                                        tick,
                                        new Quatf(values[base], values[base + 1], values[base + 2], values[base + 3]).normalize(),
                                        interpolationMode
                                )
                        );
                    }
                    else if ("scale".equals(targetPath) && outputArity >= 3) {
                        builder.scale.add(
                                new Keyframe<>(
                                        tick,
                                        new Vec3f(values[base], values[base + 1], values[base + 2]),
                                        interpolationMode
                                )
                        );
                    }
                }
            }

            Map<String, BoneAnimationTrack> trackMap = new LinkedHashMap<>();

            for (Map.Entry<String, TrackBuilder> trackEntry : tracks.entrySet()) {
                trackMap.put(trackEntry.getKey(), trackEntry.getValue().build());
            }

            clipsByName.put(clipName, new AnimationClip(clipName, maxTick, options.defaultLooping(), trackMap));
        }

        return clipsByName;
    }

    private static String uniqueClipName(Map<String, AnimationClip> clipsByName, String baseName) {
        if (!clipsByName.containsKey(baseName))
            return baseName;

        int suffix = 1;

        while (clipsByName.containsKey(baseName + "_" + suffix)) {
            suffix++;
        }

        return baseName + "_" + suffix;
    }

    private static String normalizeTargetPath(String path) {
        String normalized = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);

        if ("translation".equals(normalized) || "rotation".equals(normalized) || "scale".equals(normalized))
            return normalized;

        return null;
    }

    private static InterpolationMode parseInterpolationMode(String interpolation) {
        return "STEP".equalsIgnoreCase(interpolation) ? InterpolationMode.STEP : InterpolationMode.LINEAR;
    }

    private static Vec3f parseVector(JsonArray array, Vec3f fallback, float multiplier) {
        if (array == null || array.size() < 3)
            return fallback;

        return new Vec3f(
                parseFloat(array.get(0), fallback.x()) * multiplier,
                parseFloat(array.get(1), fallback.y()) * multiplier,
                parseFloat(array.get(2), fallback.z()) * multiplier
        );
    }

    private static Quatf parseQuaternion(JsonArray array, Quatf fallback) {
        if (array == null || array.size() < 4)
            return fallback;

        return new Quatf(
                parseFloat(array.get(0), fallback.x()),
                parseFloat(array.get(1), fallback.y()),
                parseFloat(array.get(2), fallback.z()),
                parseFloat(array.get(3), fallback.w())
        ).normalize();
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray getArray(JsonObject object, String fieldName) {
        JsonElement element = object.get(fieldName);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonObject getObject(JsonObject object, String fieldName) {
        JsonElement element = object.get(fieldName);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String getString(JsonObject object, String fieldName, String fallback) {
        JsonElement element = object.get(fieldName);

        if (element == null || element.isJsonNull())
            return fallback;

        try {
            return element.getAsString();
        }
        catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int parseInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull())
            return fallback;

        try {
            return element.getAsInt();
        }
        catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(JsonElement element, float fallback) {
        if (element == null || element.isJsonNull())
            return fallback;

        try {
            return element.getAsFloat();
        }
        catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private record NodeInfo(String name, int parentIndex, Vec3f pivot, Transform bindTransform) {
    }

    private static final class TrackBuilder {
        private final List<Keyframe<Vec3f>> translation = new ArrayList<>();
        private final List<Keyframe<Quatf>> rotation = new ArrayList<>();
        private final List<Keyframe<Vec3f>> scale = new ArrayList<>();

        private BoneAnimationTrack build() {
            return new BoneAnimationTrack(
                    new AnimationChannel<>(this.translation, Interpolators.VEC3, Vec3f.ZERO),
                    new AnimationChannel<>(this.rotation, Interpolators.QUAT, Quatf.IDENTITY),
                    new AnimationChannel<>(this.scale, Interpolators.VEC3, Vec3f.ONE)
            );
        }
    }

    private static final class DocumentContext {
        private static final int GLTF_COMPONENT_FLOAT = 5126;

        private final JsonArray bufferViews;
        private final JsonArray accessors;
        private final List<byte[]> buffers;

        private DocumentContext(JsonObject root, Path baseDirectory) {
            this.bufferViews = getArray(root, "bufferViews");
            this.accessors = getArray(root, "accessors");
            this.buffers = readBuffers(getArray(root, "buffers"), baseDirectory);
        }

        private float[] readAccessorFloats(int accessorIndex) {
            if (this.accessors == null || accessorIndex < 0 || accessorIndex >= this.accessors.size())
                throw new ModelImportException("Invalid accessor index: " + accessorIndex);

            JsonObject accessor = asObject(this.accessors.get(accessorIndex));

            if (accessor == null)
                throw new ModelImportException("Accessor " + accessorIndex + " is not an object.");

            int componentType = parseInt(accessor.get("componentType"), -1);

            if (componentType != GLTF_COMPONENT_FLOAT)
                throw new ModelImportException("Unsupported glTF accessor componentType: " + componentType + ". Only FLOAT is supported.");

            int count = parseInt(accessor.get("count"), 0);
            int arity = accessorArity(accessorIndex);
            int bufferViewIndex = parseInt(accessor.get("bufferView"), -1);
            int accessorByteOffset = parseInt(accessor.get("byteOffset"), 0);

            if (count <= 0 || arity <= 0 || bufferViewIndex < 0)
                return new float[0];

            JsonObject bufferView = bufferView(bufferViewIndex);
            int bufferIndex = parseInt(bufferView.get("buffer"), -1);

            if (bufferIndex < 0 || bufferIndex >= this.buffers.size())
                throw new ModelImportException("Invalid glTF buffer index: " + bufferIndex);

            byte[] bufferBytes = this.buffers.get(bufferIndex);
            int viewByteOffset = parseInt(bufferView.get("byteOffset"), 0);
            int byteStride = parseInt(bufferView.get("byteStride"), arity * 4);

            if (byteStride <= 0)
                byteStride = arity * 4;

            float[] values = new float[count * arity];
            ByteBuffer byteBuffer = ByteBuffer.wrap(bufferBytes).order(ByteOrder.LITTLE_ENDIAN);

            for (int frameIndex = 0; frameIndex < count; frameIndex++) {
                int frameBase = viewByteOffset + accessorByteOffset + frameIndex * byteStride;

                for (int component = 0; component < arity; component++) {
                    int byteIndex = frameBase + component * 4;

                    if (byteIndex + 4 > bufferBytes.length)
                        throw new ModelImportException("Accessor read overflow for accessor " + accessorIndex);

                    values[frameIndex * arity + component] = byteBuffer.getFloat(byteIndex);
                }
            }

            return values;
        }

        private int accessorArity(int accessorIndex) {
            JsonObject accessor = asObject(this.accessors.get(accessorIndex));

            if (accessor == null)
                return 0;

            String type = getString(accessor, "type", "SCALAR").toUpperCase(Locale.ROOT);

            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                default -> throw new ModelImportException("Unsupported glTF accessor type: " + type);
            };
        }

        private JsonObject bufferView(int bufferViewIndex) {
            if (this.bufferViews == null || bufferViewIndex < 0 || bufferViewIndex >= this.bufferViews.size())
                throw new ModelImportException("Invalid bufferView index: " + bufferViewIndex);

            JsonObject bufferView = asObject(this.bufferViews.get(bufferViewIndex));

            if (bufferView == null)
                throw new ModelImportException("bufferView " + bufferViewIndex + " is not an object.");

            return bufferView;
        }

        private static List<byte[]> readBuffers(JsonArray buffersArray, Path baseDirectory) {
            List<byte[]> buffers = new ArrayList<>();

            if (buffersArray == null)
                return buffers;

            for (JsonElement element : buffersArray) {
                JsonObject bufferObject = asObject(element);

                if (bufferObject == null) {
                    buffers.add(new byte[0]);
                    continue;
                }

                String uri = getString(bufferObject, "uri", null);

                if (uri == null || uri.isBlank()) {
                    buffers.add(new byte[0]);
                    continue;
                }

                buffers.add(readBufferUri(uri, baseDirectory));
            }

            return buffers;
        }

        private static byte[] readBufferUri(String uri, Path baseDirectory) {
            if (uri.startsWith("data:")) {
                int commaIndex = uri.indexOf(',');

                if (commaIndex < 0)
                    throw new ModelImportException("Invalid data URI in glTF buffer.");

                String metadata = uri.substring(0, commaIndex).toLowerCase(Locale.ROOT);
                String dataPart = uri.substring(commaIndex + 1);

                if (!metadata.contains(";base64"))
                    throw new ModelImportException("Unsupported data URI encoding in glTF buffer (base64 required).");

                try {
                    return Base64.getDecoder().decode(dataPart);
                }
                catch (IllegalArgumentException exception) {
                    throw new ModelImportException("Invalid base64 data URI in glTF buffer.", exception);
                }
            }

            if (baseDirectory == null)
                throw new ModelImportException("glTF buffer uses external URI but import was not file-based: " + uri);

            Path bufferPath = baseDirectory.resolve(uri).normalize();

            try {
                return Files.readAllBytes(bufferPath);
            }
            catch (IOException exception) {
                throw new ModelImportException("Failed to read external glTF buffer: " + bufferPath, exception);
            }
        }
    }
}
