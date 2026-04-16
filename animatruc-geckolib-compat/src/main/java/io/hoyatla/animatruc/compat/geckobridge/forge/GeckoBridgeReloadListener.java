package io.hoyatla.animatruc.compat.geckobridge.forge;

import io.hoyatla.animatruc.compat.geckobridge.bridge.GeckoAssetCache;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class GeckoBridgeReloadListener extends SimplePreparableReloadListener<Void> {
    private final GeckoAssetCache cache;

    public GeckoBridgeReloadListener(GeckoAssetCache cache) {
        this.cache = cache;
    }

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.cache.clear();
    }
}
