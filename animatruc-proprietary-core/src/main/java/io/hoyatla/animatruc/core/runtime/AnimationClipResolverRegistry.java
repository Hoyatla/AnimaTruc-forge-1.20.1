package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.animation.AnimationClip;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight extension point for external clip providers (for example bridge modules).
 */
public final class AnimationClipResolverRegistry {
    @FunctionalInterface
    public interface ClipResolver {
        AnimationClip resolve(Object context, String clipName);
    }

    private static final List<NamedResolver> RESOLVERS = new CopyOnWriteArrayList<>();

    private AnimationClipResolverRegistry() {
    }

    public static void register(String resolverId, ClipResolver resolver) {
        String safeId = Objects.requireNonNull(resolverId, "resolverId").trim();
        ClipResolver safeResolver = Objects.requireNonNull(resolver, "resolver");

        if (safeId.isEmpty())
            throw new IllegalArgumentException("resolverId must not be blank");

        unregister(safeId);
        RESOLVERS.add(new NamedResolver(safeId, safeResolver));
    }

    public static void unregister(String resolverId) {
        if (resolverId == null)
            return;

        String safeId = resolverId.trim();

        if (safeId.isEmpty())
            return;

        RESOLVERS.removeIf(entry -> entry.id.equals(safeId));
    }

    public static void clear() {
        RESOLVERS.clear();
    }

    public static AnimationClip resolve(Object context, String clipName) {
        if (clipName == null || clipName.isBlank())
            return null;

        for (NamedResolver entry : RESOLVERS) {
            AnimationClip clip = entry.resolver.resolve(context, clipName);
            if (clip != null)
                return clip;
        }

        return null;
    }

    private record NamedResolver(String id, ClipResolver resolver) {
    }
}
