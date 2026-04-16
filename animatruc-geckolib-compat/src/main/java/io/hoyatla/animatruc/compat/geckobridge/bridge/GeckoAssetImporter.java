package io.hoyatla.animatruc.compat.geckobridge.bridge;

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
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GeckoAssetImporter {
    private static final float TICKS_PER_SECOND = 20f;

    public AnimationAssetPack importAssets(String modelPayload, String animationPayload) {
        JsonObject modelRoot = parseRoot(modelPayload, "model");
        JsonObject animationRoot = parseRoot(animationPayload, "animation");

        return new AnimationAssetPack(parseSkeleton(modelRoot), parseClips(animationRoot));
    }

    private static JsonObject parseRoot(String payload, String kind) {
        if (payload == null || payload.isBlank())
            throw new IllegalArgumentException("Missing Gecko " + kind + " payload");

        try {
            JsonElement element = JsonParser.parseString(payload);

            if (!element.isJsonObject())
                throw new IllegalArgumentException("Invalid Gecko " + kind + " root payload");

            return element.getAsJsonObject();
        }
        catch (JsonParseException exception) {
            throw new IllegalArgumentException("Invalid Gecko " + kind + " JSON payload", exception);
        }
    }

    private static ModelSkeleton parseSkeleton(JsonObject root) {
        JsonArray geometryArray = getArray(root, "minecraft:geometry");
        JsonObject geometryRoot = geometryArray != null && !geometryArray.isEmpty()
                ? asObject(geometryArray.get(0))
                : root;

        JsonArray bonesArray = geometryRoot == null ? null : getArray(geometryRoot, "bones");
        Map<String, ModelBone> deduplicated = new LinkedHashMap<>();

        if (bonesArray != null) {
            for (JsonElement element : bonesArray) {
                JsonObject boneObject = asObject(element);

                if (boneObject == null)
                    continue;

                String name = getString(boneObject, "name", null);
                if (name == null || name.isBlank())
                    continue;

                if (deduplicated.containsKey(name))
                    continue;

                String parent = getString(boneObject, "parent", null);
                Vec3f pivot = parseVec3(boneObject.get("pivot"), Vec3f.ZERO, 1f);
                Vec3f rotation = parseVec3(boneObject.get("rotation"), Vec3f.ZERO, 1f);
                Vec3f scale = parseVec3(boneObject.get("scale"), Vec3f.ONE, 1f);

                Transform bindPose = new Transform(
                        Vec3f.ZERO,
                        Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()),
                        scale
                );

                deduplicated.put(name, new ModelBone(name, parent, pivot, bindPose));
            }
        }

        return new ModelSkeleton(new ArrayList<>(deduplicated.values()));
    }

    private static Map<String, AnimationClip> parseClips(JsonObject root) {
        JsonObject animationsObject = getObject(root, "animations");
        Map<String, AnimationClip> clips = new LinkedHashMap<>();

        if (animationsObject == null)
            return clips;

        for (Map.Entry<String, JsonElement> animationEntry : animationsObject.entrySet()) {
            JsonObject clipObject = asObject(animationEntry.getValue());

            if (clipObject == null)
                continue;

            String clipName = animationEntry.getKey();
            boolean looping = parseLooping(clipObject.get("loop"));
            float declaredLengthTicks = parseFloat(clipObject.get("animation_length"), 0f) * TICKS_PER_SECOND;
            JsonObject bonesObject = getObject(clipObject, "bones");
            Map<String, BoneAnimationTrack> tracks = new LinkedHashMap<>();
            float inferredLengthTicks = 0f;

            if (bonesObject != null) {
                for (Map.Entry<String, JsonElement> boneEntry : bonesObject.entrySet()) {
                    JsonObject boneObject = asObject(boneEntry.getValue());

                    if (boneObject == null)
                        continue;

                    TrackBuilder trackBuilder = new TrackBuilder();
                    trackBuilder.note(parseVectorChannel(boneObject.get("position"), trackBuilder.translation, Vec3f.ZERO, 1f));
                    trackBuilder.note(parseRotationChannel(boneObject.get("rotation"), trackBuilder.rotation));
                    trackBuilder.note(parseVectorChannel(boneObject.get("scale"), trackBuilder.scale, Vec3f.ONE, 1f));

                    inferredLengthTicks = Math.max(inferredLengthTicks, trackBuilder.maxTick);
                    tracks.put(boneEntry.getKey(), trackBuilder.build());
                }
            }

            float lengthTicks = Math.max(declaredLengthTicks, inferredLengthTicks);
            clips.put(clipName, new AnimationClip(clipName, lengthTicks, looping, tracks));
        }

        return clips;
    }

    private static float parseVectorChannel(
            JsonElement channelElement,
            List<Keyframe<Vec3f>> destination,
            Vec3f fallback,
            float multiplier) {
        if (channelElement == null || channelElement.isJsonNull())
            return 0f;

        float maxTick = 0f;

        if (channelElement.isJsonArray()) {
            destination.add(new Keyframe<>(0f, parseVec3(channelElement, fallback, multiplier), InterpolationMode.LINEAR));
            return 0f;
        }

        JsonObject channelObject = asObject(channelElement);

        if (channelObject == null)
            return 0f;

        if (isStaticVectorNode(channelObject)) {
            destination.add(new Keyframe<>(0f, parseChannelVector(channelObject, fallback, multiplier), parseInterpolation(channelObject)));
            return 0f;
        }

        for (Map.Entry<String, JsonElement> keyframeEntry : channelObject.entrySet()) {
            float tick = Math.max(0f, parseFloatSafe(keyframeEntry.getKey()) * TICKS_PER_SECOND);
            InterpolationMode interpolation = parseInterpolation(asObject(keyframeEntry.getValue()));
            Vec3f value = parseChannelVector(keyframeEntry.getValue(), fallback, multiplier);

            destination.add(new Keyframe<>(tick, value, interpolation));
            maxTick = Math.max(maxTick, tick);
        }

        destination.sort(Comparator.comparing(Keyframe::tick));
        return maxTick;
    }

    private static float parseRotationChannel(JsonElement channelElement, List<Keyframe<Quatf>> destination) {
        if (channelElement == null || channelElement.isJsonNull())
            return 0f;

        float maxTick = 0f;

        if (channelElement.isJsonArray()) {
            Vec3f rotation = parseVec3(channelElement, Vec3f.ZERO, 1f);
            destination.add(new Keyframe<>(0f, Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()), InterpolationMode.LINEAR));
            return 0f;
        }

        JsonObject channelObject = asObject(channelElement);

        if (channelObject == null)
            return 0f;

        if (isStaticVectorNode(channelObject)) {
            Vec3f rotation = parseChannelVector(channelObject, Vec3f.ZERO, 1f);
            destination.add(new Keyframe<>(0f, Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()), parseInterpolation(channelObject)));
            return 0f;
        }

        for (Map.Entry<String, JsonElement> keyframeEntry : channelObject.entrySet()) {
            float tick = Math.max(0f, parseFloatSafe(keyframeEntry.getKey()) * TICKS_PER_SECOND);
            JsonObject keyframeObject = asObject(keyframeEntry.getValue());
            Vec3f rotation = parseChannelVector(keyframeEntry.getValue(), Vec3f.ZERO, 1f);
            InterpolationMode interpolation = parseInterpolation(keyframeObject);

            destination.add(new Keyframe<>(tick, Quatf.fromEulerDegrees(rotation.x(), rotation.y(), rotation.z()), interpolation));
            maxTick = Math.max(maxTick, tick);
        }

        destination.sort(Comparator.comparing(Keyframe::tick));
        return maxTick;
    }

    private static boolean isStaticVectorNode(JsonObject object) {
        return object.has("vector")
                || object.has("post")
                || object.has("x")
                || object.has("y")
                || object.has("z");
    }

    private static Vec3f parseChannelVector(JsonElement keyframeElement, Vec3f fallback, float multiplier) {
        if (keyframeElement == null || keyframeElement.isJsonNull())
            return fallback;

        if (keyframeElement.isJsonArray())
            return parseVec3(keyframeElement, fallback, multiplier);

        JsonObject object = asObject(keyframeElement);

        if (object == null)
            return fallback;

        if (object.has("post"))
            return parseVec3(object.get("post"), fallback, multiplier);
        if (object.has("vector"))
            return parseVec3(object.get("vector"), fallback, multiplier);

        return parseVec3(object, fallback, multiplier);
    }

    private static boolean parseLooping(JsonElement loopElement) {
        if (loopElement == null || loopElement.isJsonNull())
            return false;

        if (loopElement.isJsonPrimitive()) {
            try {
                if (loopElement.getAsJsonPrimitive().isBoolean())
                    return loopElement.getAsBoolean();

                String value = loopElement.getAsString().trim().toLowerCase(Locale.ROOT);
                if ("loop".equals(value) || "true".equals(value))
                    return true;
                if ("once".equals(value) || "false".equals(value) || "hold".equals(value) || "hold_on_last_frame".equals(value))
                    return false;
            }
            catch (RuntimeException ignored) {
                return false;
            }
        }

        return false;
    }

    private static InterpolationMode parseInterpolation(JsonObject keyframeObject) {
        if (keyframeObject == null)
            return InterpolationMode.LINEAR;

        String interpolation = getString(keyframeObject, "lerp_mode", getString(keyframeObject, "interpolation", "linear"));
        String normalized = interpolation == null ? "linear" : interpolation.trim().toLowerCase(Locale.ROOT);

        return ("step".equals(normalized) || "hold".equals(normalized)) ? InterpolationMode.STEP : InterpolationMode.LINEAR;
    }

    private static Vec3f parseVec3(JsonElement element, Vec3f fallback, float multiplier) {
        if (element == null || element.isJsonNull())
            return fallback;

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();

            if (array.size() < 3)
                return fallback;

            return new Vec3f(
                    parseFloat(array.get(0), fallback.x()) * multiplier,
                    parseFloat(array.get(1), fallback.y()) * multiplier,
                    parseFloat(array.get(2), fallback.z()) * multiplier
            );
        }

        JsonObject object = asObject(element);

        if (object == null)
            return fallback;

        return new Vec3f(
                parseFloat(object.get("x"), fallback.x()) * multiplier,
                parseFloat(object.get("y"), fallback.y()) * multiplier,
                parseFloat(object.get("z"), fallback.z()) * multiplier
        );
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonObject getObject(JsonObject object, String fieldName) {
        return asObject(object.get(fieldName));
    }

    private static JsonArray getArray(JsonObject object, String fieldName) {
        JsonElement element = object.get(fieldName);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
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

    private static float parseFloatSafe(String raw) {
        if (raw == null || raw.isBlank())
            return 0f;

        try {
            return Float.parseFloat(raw);
        }
        catch (NumberFormatException ignored) {
            return 0f;
        }
    }

    private static final class TrackBuilder {
        private final List<Keyframe<Vec3f>> translation = new ArrayList<>();
        private final List<Keyframe<Quatf>> rotation = new ArrayList<>();
        private final List<Keyframe<Vec3f>> scale = new ArrayList<>();
        private float maxTick;

        private void note(float tick) {
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
