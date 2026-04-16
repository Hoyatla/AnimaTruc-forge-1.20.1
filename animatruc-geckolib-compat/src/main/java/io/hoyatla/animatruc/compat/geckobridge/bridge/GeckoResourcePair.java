package io.hoyatla.animatruc.compat.geckobridge.bridge;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record GeckoResourcePair(ResourceLocation modelResource, ResourceLocation animationResource) {
    public GeckoResourcePair {
        Objects.requireNonNull(modelResource, "modelResource");
        Objects.requireNonNull(animationResource, "animationResource");
    }

    public static GeckoResourcePair of(ResourceLocation modelResource, ResourceLocation animationResource) {
        return new GeckoResourcePair(
                normalize(modelResource, "geo/", ".geo.json"),
                normalize(animationResource, "animations/", ".animation.json")
        );
    }

    private static ResourceLocation normalize(ResourceLocation input, String defaultPrefix, String defaultSuffix) {
        String path = input.getPath();

        if (!path.startsWith(defaultPrefix))
            path = defaultPrefix + path;
        if (!path.endsWith(defaultSuffix))
            path = path + defaultSuffix;

        ResourceLocation normalized = ResourceLocation.tryBuild(input.getNamespace(), path);

        if (normalized == null)
            throw new IllegalArgumentException("Invalid Gecko resource location: " + input);

        return normalized;
    }
}
