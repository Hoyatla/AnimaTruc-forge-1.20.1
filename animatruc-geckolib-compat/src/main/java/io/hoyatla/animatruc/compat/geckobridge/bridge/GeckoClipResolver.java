package io.hoyatla.animatruc.compat.geckobridge.bridge;

import io.hoyatla.animatruc.compat.geckobridge.forge.GeckoBridgeConfig;
import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.asset.AnimationAssetPack;
import io.hoyatla.animatruc.core.runtime.AnimationClipResolverRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GeckoClipResolver implements AnimationClipResolverRegistry.ClipResolver {
    private static final Logger LOGGER = LogManager.getLogger();

    private final GeckoResourceLocator locator;
    private final GeckoAssetCache cache;
    private final GeckoResourceLoader resourceLoader;
    private final GeckoAssetImporter importer;
    private final Set<String> warnedLookupFailures = ConcurrentHashMap.newKeySet();
    private final Set<String> warnedImportFailures = ConcurrentHashMap.newKeySet();
    private final Set<String> warnedMissingClips = ConcurrentHashMap.newKeySet();

    public GeckoClipResolver(
            GeckoResourceLocator locator,
            GeckoAssetCache cache,
            GeckoResourceLoader resourceLoader,
            GeckoAssetImporter importer) {
        this.locator = locator;
        this.cache = cache;
        this.resourceLoader = resourceLoader;
        this.importer = importer;
    }

    @Override
    public AnimationClip resolve(Object context, String clipName) {
        if (!GeckoBridgeConfig.ENABLE_BRIDGE.get() || context == null || clipName == null || clipName.isBlank())
            return null;

        GeckoResourcePair pair = this.locator.locate(context);
        if (pair == null)
            return null;

        AnimationAssetPack pack = this.cache.getOrLoad(pair, () -> importPair(pair));
        if (pack == null)
            return null;

        AnimationClip clip = resolveClipFromPack(pack, clipName.trim());
        if (clip != null) {
            if (GeckoBridgeConfig.ENABLE_DEBUG_LOGS.get()) {
                LOGGER.debug(
                        "Resolved Gecko clip '{}' for {} via model {}",
                        clip.name(),
                        context.getClass().getName(),
                        pair.modelResource()
                );
            }

            return clip;
        }

        warnMissingClipOnce(pair, clipName);
        return null;
    }

    private AnimationAssetPack importPair(GeckoResourcePair pair) {
        String modelPayload = this.resourceLoader.loadJson(pair.modelResource());
        String animationPayload = this.resourceLoader.loadJson(pair.animationResource());

        if (modelPayload == null || animationPayload == null) {
            warnLookupFailureOnce(pair, modelPayload == null, animationPayload == null);
            return null;
        }

        try {
            AnimationAssetPack pack = this.importer.importAssets(modelPayload, animationPayload);

            if (GeckoBridgeConfig.ENABLE_DEBUG_LOGS.get()) {
                LOGGER.debug(
                        "Imported Gecko assets model={} animation={} clips={}",
                        pair.modelResource(),
                        pair.animationResource(),
                        pack.clipsByName().size()
                );
            }

            return pack;
        }
        catch (RuntimeException exception) {
            warnImportFailureOnce(pair, exception);
            return null;
        }
    }

    private static AnimationClip resolveClipFromPack(AnimationAssetPack pack, String clipName) {
        AnimationClip direct = pack.clip(clipName);

        if (direct != null)
            return direct;

        String prefixed = clipName.startsWith("animation.") ? clipName : "animation." + clipName;
        AnimationClip prefixedClip = pack.clip(prefixed);

        if (prefixedClip != null)
            return prefixedClip;

        String suffix = lastSegment(clipName);

        for (AnimationClip clip : pack.clipsByName().values()) {
            if (lastSegment(clip.name()).equals(suffix))
                return clip;
        }

        return null;
    }

    private static String lastSegment(String value) {
        int index = value.lastIndexOf('.');
        return index >= 0 && index < value.length() - 1 ? value.substring(index + 1) : value;
    }

    private void warnLookupFailureOnce(GeckoResourcePair pair, boolean missingModel, boolean missingAnimation) {
        String key = pair.modelResource() + "|" + pair.animationResource() + "|lookup";
        if (!this.warnedLookupFailures.add(key))
            return;

        if (missingModel && missingAnimation) {
            LOGGER.warn(
                    "Gecko bridge could not find model {} and animation {} resources. Returning fallback null.",
                    pair.modelResource(),
                    pair.animationResource()
            );
            return;
        }

        if (missingModel) {
            LOGGER.warn(
                    "Gecko bridge could not find model resource {}. Returning fallback null.",
                    pair.modelResource()
            );
            return;
        }

        LOGGER.warn(
                "Gecko bridge could not find animation resource {}. Returning fallback null.",
                pair.animationResource()
        );
    }

    private void warnImportFailureOnce(GeckoResourcePair pair, RuntimeException exception) {
        String key = pair.modelResource() + "|" + pair.animationResource() + "|import";
        if (!this.warnedImportFailures.add(key))
            return;

        LOGGER.warn(
                "Gecko bridge failed to convert resources model={} animation={}. Returning fallback null.",
                pair.modelResource(),
                pair.animationResource(),
                exception
        );
    }

    private void warnMissingClipOnce(GeckoResourcePair pair, String clipName) {
        String key = pair.modelResource() + "|" + pair.animationResource() + "|" + clipName;
        if (!this.warnedMissingClips.add(key))
            return;

        LOGGER.warn(
                "Gecko bridge could not resolve clip '{}' from animation {}. Returning fallback null.",
                clipName,
                pair.animationResource()
        );
    }
}
