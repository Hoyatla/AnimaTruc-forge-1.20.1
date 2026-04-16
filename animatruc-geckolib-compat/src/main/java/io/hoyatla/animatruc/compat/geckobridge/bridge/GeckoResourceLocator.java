package io.hoyatla.animatruc.compat.geckobridge.bridge;

import io.hoyatla.animatruc.compat.geckobridge.forge.GeckoBridgeConfig;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeckoResourceLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String[] MODEL_METHOD_NAMES = {
            "getModelResource",
            "getModelLocation",
            "getGeoModelResource",
            "modelResource"
    };

    private static final String[] ANIMATION_METHOD_NAMES = {
            "getAnimationResource",
            "getAnimationLocation",
            "animationResource"
    };

    private static final String[] MODEL_FIELD_NAMES = {
            "MODEL",
            "MODEL_LOCATION",
            "GEO_MODEL",
            "GEO_MODEL_LOCATION"
    };

    private static final String[] ANIMATION_FIELD_NAMES = {
            "ANIMATION",
            "ANIMATION_LOCATION",
            "ANIMATION_FILE",
            "ANIMATION_FILE_LOCATION"
    };

    private final Map<Class<?>, GeckoResourcePair> explicitMappings = new ConcurrentHashMap<>();
    private final Set<String> warnedAnimatableTypes = ConcurrentHashMap.newKeySet();

    public void registerMapping(Class<?> animatableType, ResourceLocation modelResource, ResourceLocation animationResource) {
        if (animatableType == null || modelResource == null || animationResource == null)
            return;

        this.explicitMappings.put(animatableType, GeckoResourcePair.of(modelResource, animationResource));
    }

    public void clearMappings() {
        this.explicitMappings.clear();
        this.warnedAnimatableTypes.clear();
    }

    public GeckoResourcePair locate(Object animatable) {
        if (animatable == null)
            return null;

        Class<?> animatableType = animatable.getClass();
        GeckoResourcePair mapped = findExplicitMapping(animatableType);

        if (mapped != null)
            return mapped;

        if (!GeckoBridgeConfig.ENABLE_REFLECTION_LOCATOR.get())
            return null;

        ResourceLocation model = invokeGetter(animatable, MODEL_METHOD_NAMES);
        ResourceLocation animation = invokeGetter(animatable, ANIMATION_METHOD_NAMES);

        if (model == null)
            model = readStaticField(animatableType, MODEL_FIELD_NAMES);
        if (animation == null)
            animation = readStaticField(animatableType, ANIMATION_FIELD_NAMES);

        if (model == null || animation == null) {
            warnMissingMappingOnce(animatableType);
            return null;
        }

        return GeckoResourcePair.of(model, animation);
    }

    private GeckoResourcePair findExplicitMapping(Class<?> animatableType) {
        Class<?> cursor = animatableType;

        while (cursor != null && cursor != Object.class) {
            GeckoResourcePair pair = this.explicitMappings.get(cursor);
            if (pair != null)
                return pair;

            cursor = cursor.getSuperclass();
        }

        for (Map.Entry<Class<?>, GeckoResourcePair> entry : this.explicitMappings.entrySet()) {
            if (entry.getKey().isAssignableFrom(animatableType))
                return entry.getValue();
        }

        return null;
    }

    private ResourceLocation invokeGetter(Object target, String[] candidateNames) {
        Class<?> type = target.getClass();

        for (String methodName : candidateNames) {
            for (Method method : type.getMethods()) {
                if (!methodName.equals(method.getName()))
                    continue;
                if (method.getParameterCount() > 1)
                    continue;

                Object value = invokeMethod(target, method);
                ResourceLocation resolved = toResourceLocation(value);
                if (resolved != null)
                    return resolved;
            }
        }

        return null;
    }

    private static Object invokeMethod(Object target, Method method) {
        try {
            if (method.getParameterCount() == 0)
                return method.invoke(target);

            Class<?> parameterType = method.getParameterTypes()[0];
            if (parameterType.isAssignableFrom(target.getClass()))
                return method.invoke(target, target);

            return null;
        }
        catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
            return null;
        }
    }

    private ResourceLocation readStaticField(Class<?> type, String[] candidateNames) {
        Class<?> cursor = type;

        while (cursor != null && cursor != Object.class) {
            for (String fieldName : candidateNames) {
                try {
                    Field field = cursor.getDeclaredField(fieldName);
                    if (!Modifier.isStatic(field.getModifiers()))
                        continue;

                    field.setAccessible(true);
                    ResourceLocation resolved = toResourceLocation(field.get(null));
                    if (resolved != null)
                        return resolved;
                }
                catch (NoSuchFieldException | IllegalAccessException ignored) {
                    // Try next field candidate.
                }
            }

            cursor = cursor.getSuperclass();
        }

        return null;
    }

    private static ResourceLocation toResourceLocation(Object value) {
        if (value instanceof ResourceLocation location)
            return location;
        if (value instanceof String stringValue)
            return ResourceLocation.tryParse(stringValue.trim());

        return null;
    }

    private void warnMissingMappingOnce(Class<?> animatableType) {
        String key = animatableType.getName();
        if (!this.warnedAnimatableTypes.add(key))
            return;

        LOGGER.warn(
                "AnimaTruc Gecko bridge could not locate Gecko resources for {}. Register explicit mapping via bridge API if needed.",
                animatableType.getName()
        );
    }
}
