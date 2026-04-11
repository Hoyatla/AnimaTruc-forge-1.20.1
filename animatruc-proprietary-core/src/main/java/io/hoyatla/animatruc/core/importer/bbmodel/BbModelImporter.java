package io.hoyatla.animatruc.core.importer.bbmodel;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Imports Blockbench .bbmodel skeletons and animation clips into AnimaTruc runtime data.
 */
public final class BbModelImporter implements AnimationAssetImporter {
    @Override
    public AnimationAssetPack importFromString(String payload, ModelImportOptions options) {
        Objects.requireNonNull(payload, "payload");
        ModelImportOptions safeOptions = options == null ? ModelImportOptions.DEFAULT : options;

        JsonObject root = parseRoot(payload);
        Map<String, String> boneNameByUuid = new LinkedHashMap<>();
        ModelSkeleton skeleton = parseSkeleton(root, safeOptions, boneNameByUuid);
        Map<String, AnimationClip> clips = parseClips(root, safeOptions, boneNameByUuid);

        return new AnimationAssetPack(skeleton, clips);
    }

    private static JsonObject parseRoot(String payload) {
        try {
            JsonElement element = JsonParser.parseString(payload);

            if (!element.isJsonObject())
                throw new ModelImportException("Invalid .bbmodel content: root must be an object.");

            return element.getAsJsonObject();
        }
        catch (JsonParseException exception) {
            throw new ModelImportException("Invalid .bbmodel JSON.", exception);
        }
    }

    private static ModelSkeleton parseSkeleton(
            JsonObject root,
            ModelImportOptions options,
            Map<String, String> boneNameByUuid) {
        List<RawBone> rawBones = new ArrayList<>();

        JsonArray bonesArray = getArray(root, "bones");

        if (bonesArray != null && !bonesArray.isEmpty()) {
            parseBoneArray(bonesArray, options.translationScale(), rawBones);
        }
        else {
            JsonArray outliner = getArray(root, "outliner");

            if (outliner != null) {
                for (JsonElement element : outliner) {
                    parseOutlinerNode(element, null, options.translationScale(), rawBones);
                }
            }
        }

        List<ModelBone> orderedBones = new ArrayList<>(rawBones.size());

        for (RawBone rawBone : rawBones) {
            String parentName = resolveParentName(rawBone, rawBones);
            ModelBone modelBone = new ModelBone(rawBone.name, parentName, rawBone.pivot, rawBone.bindTransform);
            orderedBones.add(modelBone);

            if (!rawBone.uuid.isEmpty())
                boneNameByUuid.put(rawBone.uuid, rawBone.name);
        }

        return new ModelSkeleton(orderedBones);
    }

    private static void parseBoneArray(JsonArray bonesArray, float translationScale, List<RawBone> destination) {
        for (JsonElement element : bonesArray) {
            if (!element.isJsonObject())
                continue;

            JsonObject boneObject = element.getAsJsonObject();
            String name = getString(boneObject, "name", null);

            if (name == null || name.isBlank())
                continue;

            String uuid = getString(boneObject, "uuid", name);
            String parentRef = getString(boneObject, "parent", null);
            Vec3f pivot = parseVector(
                    firstNonNullArray(boneObject, "pivot", "origin"),
                    Vec3f.ZERO,
                    translationScale
            );
            Vec3f rotation = parseVector(getArray(boneObject, "rotation"), Vec3f.ZERO, 1f);
            Vec3f scale = parseVector(getArray(boneObject, "scale"), Vec3f.ONE, 1f);
            Transform bindTransform = new Transform(Vec3f.ZERO, Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()), scale);

            destination.add(new RawBone(name, uuid, parentRef, pivot, bindTransform));
        }
    }

    private static void parseOutlinerNode(
            JsonElement nodeElement,
            String parentName,
            float translationScale,
            List<RawBone> destination) {
        if (!nodeElement.isJsonObject())
            return;

        JsonObject nodeObject = nodeElement.getAsJsonObject();
        String name = getString(nodeObject, "name", null);

        if (name == null || name.isBlank())
            return;

        String uuid = getString(nodeObject, "uuid", name);
        Vec3f pivot = parseVector(getArray(nodeObject, "origin"), Vec3f.ZERO, translationScale);
        Vec3f rotation = parseVector(getArray(nodeObject, "rotation"), Vec3f.ZERO, 1f);
        Vec3f scale = parseVector(getArray(nodeObject, "scale"), Vec3f.ONE, 1f);
        Transform bindTransform = new Transform(Vec3f.ZERO, Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()), scale);

        destination.add(new RawBone(name, uuid, parentName, pivot, bindTransform));

        JsonArray children = getArray(nodeObject, "children");

        if (children == null)
            return;

        for (JsonElement child : children) {
            parseOutlinerNode(child, name, translationScale, destination);
        }
    }

    private static String resolveParentName(RawBone rawBone, List<RawBone> allBones) {
        if (rawBone.parentReference == null || rawBone.parentReference.isBlank())
            return null;

        for (RawBone candidate : allBones) {
            if (candidate.uuid.equals(rawBone.parentReference) || candidate.name.equals(rawBone.parentReference))
                return candidate.name;
        }

        return rawBone.parentReference;
    }

    private static Map<String, AnimationClip> parseClips(
            JsonObject root,
            ModelImportOptions options,
            Map<String, String> boneNameByUuid) {
        Map<String, AnimationClip> clipsByName = new LinkedHashMap<>();
        JsonArray animationsArray = getArray(root, "animations");

        if (animationsArray == null)
            return clipsByName;

        int unnamedIndex = 0;

        for (JsonElement animationElement : animationsArray) {
            if (!animationElement.isJsonObject())
                continue;

            JsonObject animationObject = animationElement.getAsJsonObject();
            String clipName = getString(animationObject, "name", "clip_" + unnamedIndex++);
            float lengthTicks = Math.max(0f, getFloat(animationObject, "length", 0f) * options.ticksPerSecond());
            boolean looping = parseLooping(animationObject, options.defaultLooping());
            JsonObject animatorsObject = getObject(animationObject, "animators");

            if (animatorsObject == null) {
                clipsByName.put(clipName, new AnimationClip(clipName, lengthTicks, looping, Map.of()));
                continue;
            }

            Map<String, TrackBuilder> tracksByBone = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> animatorEntry : animatorsObject.entrySet()) {
                if (!animatorEntry.getValue().isJsonObject())
                    continue;

                JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                String type = getString(animator, "type", "bone");

                if (!"bone".equalsIgnoreCase(type))
                    continue;

                String boneName = resolveAnimatorBoneName(animatorEntry.getKey(), animator, boneNameByUuid);
                TrackBuilder builder = tracksByBone.computeIfAbsent(boneName, unused -> new TrackBuilder());
                JsonArray keyframes = getArray(animator, "keyframes");

                if (keyframes == null)
                    continue;

                for (JsonElement keyframeElement : keyframes) {
                    if (!keyframeElement.isJsonObject())
                        continue;

                    JsonObject keyframeObject = keyframeElement.getAsJsonObject();
                    String channel = normalizeChannel(getString(keyframeObject, "channel", ""));

                    if (channel == null)
                        continue;

                    float tick = Math.max(0f, getFloat(keyframeObject, "time", 0f) * options.ticksPerSecond());
                    InterpolationMode interpolationMode = parseInterpolationMode(keyframeObject);
                    Vec3f vector = parseKeyframeVector(keyframeObject, channel, options.translationScale());
                    builder.noteTick(tick);

                    if ("position".equals(channel)) {
                        builder.translation.add(new Keyframe<>(tick, vector, interpolationMode));
                    }
                    else if ("rotation".equals(channel)) {
                        builder.rotation.add(
                                new Keyframe<>(
                                        tick,
                                        Quatf.fromEulerDegrees(vector.x(), vector.y(), vector.z()),
                                        interpolationMode
                                )
                        );
                    }
                    else if ("scale".equals(channel)) {
                        builder.scale.add(new Keyframe<>(tick, vector, interpolationMode));
                    }
                }
            }

            Map<String, BoneAnimationTrack> tracks = new LinkedHashMap<>();
            float inferredLengthTicks = lengthTicks;

            for (Map.Entry<String, TrackBuilder> trackEntry : tracksByBone.entrySet()) {
                TrackBuilder trackBuilder = trackEntry.getValue();
                inferredLengthTicks = Math.max(inferredLengthTicks, trackBuilder.maxTick);
                tracks.put(trackEntry.getKey(), trackBuilder.build());
            }

            clipsByName.put(clipName, new AnimationClip(clipName, inferredLengthTicks, looping, tracks));
        }

        return clipsByName;
    }

    private static String resolveAnimatorBoneName(String animatorKey, JsonObject animator, Map<String, String> boneNameByUuid) {
        String explicitName = getString(animator, "name", null);

        if (explicitName != null && !explicitName.isBlank())
            return explicitName;

        String resolved = boneNameByUuid.get(animatorKey);

        return resolved != null ? resolved : animatorKey;
    }

    private static Vec3f parseKeyframeVector(JsonObject keyframe, String channel, float translationScale) {
        JsonArray dataPoints = getArray(keyframe, "data_points");
        Vec3f fallback = "scale".equals(channel) ? Vec3f.ONE : Vec3f.ZERO;
        float scale = "position".equals(channel) ? translationScale : 1f;

        if (dataPoints != null && !dataPoints.isEmpty()) {
            JsonElement first = dataPoints.get(0);

            if (first.isJsonObject()) {
                JsonObject point = first.getAsJsonObject();
                return new Vec3f(
                        parseFloat(point.get("x"), fallback.x()) * scale,
                        parseFloat(point.get("y"), fallback.y()) * scale,
                        parseFloat(point.get("z"), fallback.z()) * scale
                );
            }
            if (first.isJsonArray()) {
                return parseVector(first.getAsJsonArray(), fallback, scale);
            }
        }

        if (keyframe.has("x") || keyframe.has("y") || keyframe.has("z")) {
            return new Vec3f(
                    parseFloat(keyframe.get("x"), fallback.x()) * scale,
                    parseFloat(keyframe.get("y"), fallback.y()) * scale,
                    parseFloat(keyframe.get("z"), fallback.z()) * scale
            );
        }

        return fallback;
    }

    private static boolean parseLooping(JsonObject animationObject, boolean fallback) {
        String loopValue = getString(animationObject, "loop", null);

        if (loopValue == null)
            return fallback;

        String normalized = loopValue.trim().toLowerCase(Locale.ROOT);

        if ("once".equals(normalized) || "hold".equals(normalized) || "hold_on_last_frame".equals(normalized))
            return false;

        return "loop".equals(normalized) || "true".equals(normalized) || fallback;
    }

    private static InterpolationMode parseInterpolationMode(JsonObject keyframeObject) {
        String interpolation = getString(keyframeObject, "interpolation", "linear");

        return "step".equalsIgnoreCase(interpolation) ? InterpolationMode.STEP : InterpolationMode.LINEAR;
    }

    private static String normalizeChannel(String channel) {
        if (channel == null)
            return null;

        String normalized = channel.trim().toLowerCase(Locale.ROOT);

        if ("position".equals(normalized) || "translation".equals(normalized) || "location".equals(normalized))
            return "position";
        if ("rotation".equals(normalized))
            return "rotation";
        if ("scale".equals(normalized))
            return "scale";

        return null;
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

    private static float getFloat(JsonObject object, String fieldName, float fallback) {
        return parseFloat(object.get(fieldName), fallback);
    }

    private static float parseFloat(JsonElement value, float fallback) {
        if (value == null || value.isJsonNull())
            return fallback;

        try {
            return value.getAsFloat();
        }
        catch (RuntimeException ignored) {
            return fallback;
        }
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

    private static JsonObject getObject(JsonObject object, String fieldName) {
        JsonElement element = object.get(fieldName);

        if (element == null || !element.isJsonObject())
            return null;

        return element.getAsJsonObject();
    }

    private static JsonArray getArray(JsonObject object, String fieldName) {
        JsonElement element = object.get(fieldName);

        if (element == null || !element.isJsonArray())
            return null;

        return element.getAsJsonArray();
    }

    private static JsonArray firstNonNullArray(JsonObject object, String firstField, String secondField) {
        JsonArray first = getArray(object, firstField);
        return first != null ? first : getArray(object, secondField);
    }

    private record RawBone(
            String name,
            String uuid,
            String parentReference,
            Vec3f pivot,
            Transform bindTransform) {
    }

    private static final class TrackBuilder {
        private final List<Keyframe<Vec3f>> translation = new ArrayList<>();
        private final List<Keyframe<Quatf>> rotation = new ArrayList<>();
        private final List<Keyframe<Vec3f>> scale = new ArrayList<>();
        private float maxTick;

        private void noteTick(float tick) {
            this.maxTick = Math.max(this.maxTick, tick);
        }

        private BoneAnimationTrack build() {
            return new BoneAnimationTrack(
                    new AnimationChannel<>(this.translation, Interpolators.VEC3, Vec3f.ZERO),
                    new AnimationChannel<>(this.rotation, Interpolators.QUAT, Quatf.IDENTITY),
                    new AnimationChannel<>(this.scale, Interpolators.VEC3, Vec3f.ONE)
            );
        }
    }
}
