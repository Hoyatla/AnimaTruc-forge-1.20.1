package io.hoyatla.animatruc.compat.geckobridge.bridge;

import io.hoyatla.animatruc.core.runtime.AnimationClipResolverRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime wiring for the Gecko bridge components.
 */
public final class GeckoBridgeRuntime {
    public static final String RESOLVER_ID = "animatruc_geckobridge";

    private static final Logger LOGGER = LogManager.getLogger();

    private final GeckoResourceLocator locator = new GeckoResourceLocator();
    private final GeckoAssetCache cache = new GeckoAssetCache();
    private final GeckoResourceLoader resourceLoader = new GeckoResourceLoader();
    private final GeckoAssetImporter importer = new GeckoAssetImporter();
    private final GeckoClipResolver resolver = new GeckoClipResolver(this.locator, this.cache, this.resourceLoader, this.importer);

    public void registerResolver() {
        AnimationClipResolverRegistry.register(RESOLVER_ID, this.resolver);
        LOGGER.info("Registered AnimaTruc Gecko bridge clip resolver");
    }

    public void unregisterResolver() {
        AnimationClipResolverRegistry.unregister(RESOLVER_ID);
        LOGGER.info("Unregistered AnimaTruc Gecko bridge clip resolver");
    }

    public GeckoResourceLocator locator() {
        return this.locator;
    }

    public GeckoAssetCache cache() {
        return this.cache;
    }
}
