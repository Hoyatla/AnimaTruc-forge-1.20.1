package io.hoyatla.animatruc.compat.geckobridge.bridge;

import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.runtime.AnimationClipResolverRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Public bridge API for explicit mappings and diagnostics.
 */
public final class GeckoBridgeApi {
    private GeckoBridgeApi() {
    }

    public static void bootstrap(GeckoBridgeRuntime runtime) {
        GeckoBridgeServices.bind(runtime);
    }

    public static void registerAnimatableResources(Class<?> animatableType, ResourceLocation modelResource, ResourceLocation animationResource) {
        GeckoBridgeRuntime runtime = GeckoBridgeServices.runtime();

        if (runtime == null)
            return;

        runtime.locator().registerMapping(animatableType, modelResource, animationResource);
    }

    public static void clearCachedAssets() {
        GeckoBridgeRuntime runtime = GeckoBridgeServices.runtime();

        if (runtime == null)
            return;

        runtime.cache().clear();
    }

    public static void clearMappings() {
        GeckoBridgeRuntime runtime = GeckoBridgeServices.runtime();

        if (runtime == null)
            return;

        runtime.locator().clearMappings();
    }

    public static AnimationClip resolveClip(Object animatable, String clipName) {
        return AnimationClipResolverRegistry.resolve(animatable, clipName);
    }
}
