package io.hoyatla.animatruc.core.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable parameter bag used by animation graph transitions.
 */
public final class GraphParameters {
    private final Map<String, Float> floats = new HashMap<>();
    private final Map<String, Boolean> booleans = new HashMap<>();

    public void setFloat(String key, float value) {
        this.floats.put(key, value);
    }

    public float getFloat(String key, float fallback) {
        return this.floats.getOrDefault(key, fallback);
    }

    public void setBoolean(String key, boolean value) {
        this.booleans.put(key, value);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return this.booleans.getOrDefault(key, fallback);
    }

    public void clear() {
        this.floats.clear();
        this.booleans.clear();
    }
}
