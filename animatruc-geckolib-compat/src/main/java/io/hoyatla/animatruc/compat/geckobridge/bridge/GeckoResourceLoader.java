package io.hoyatla.animatruc.compat.geckobridge.bridge;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class GeckoResourceLoader {
    public String loadJson(ResourceLocation location) {
        if (location == null)
            return null;

        String classpath = "assets/" + location.getNamespace() + "/" + location.getPath();
        String payload = readFrom(ClassLoader.getSystemClassLoader(), classpath);

        if (payload != null)
            return payload;

        payload = readFrom(Thread.currentThread().getContextClassLoader(), classpath);
        if (payload != null)
            return payload;

        return readFrom(this.getClass().getClassLoader(), classpath);
    }

    private static String readFrom(ClassLoader classLoader, String classpath) {
        if (classLoader == null)
            return null;

        try (InputStream stream = classLoader.getResourceAsStream(classpath)) {
            if (stream == null)
                return null;

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            return null;
        }
    }
}
