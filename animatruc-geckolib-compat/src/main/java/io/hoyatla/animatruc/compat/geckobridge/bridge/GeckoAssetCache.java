package io.hoyatla.animatruc.compat.geckobridge.bridge;

import io.hoyatla.animatruc.core.asset.AnimationAssetPack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class GeckoAssetCache {
    private final Map<CacheKey, Optional<AnimationAssetPack>> cache = new ConcurrentHashMap<>();

    public AnimationAssetPack getOrLoad(GeckoResourcePair pair, Supplier<AnimationAssetPack> loader) {
        if (pair == null || loader == null)
            return null;

        CacheKey key = CacheKey.of(pair);

        Optional<AnimationAssetPack> resolved = this.cache.computeIfAbsent(
                key,
                ignored -> Optional.ofNullable(loader.get())
        );

        return resolved.orElse(null);
    }

    public void clear() {
        this.cache.clear();
    }

    private record CacheKey(String modelNamespace, String modelPath, String animationNamespace, String animationPath) {
        private static CacheKey of(GeckoResourcePair pair) {
            return new CacheKey(
                    pair.modelResource().getNamespace(),
                    pair.modelResource().getPath(),
                    pair.animationResource().getNamespace(),
                    pair.animationResource().getPath()
            );
        }
    }
}
