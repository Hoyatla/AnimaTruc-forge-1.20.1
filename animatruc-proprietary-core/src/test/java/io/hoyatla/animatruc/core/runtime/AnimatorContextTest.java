package io.hoyatla.animatruc.core.runtime;

import io.hoyatla.animatruc.core.math.Vec3f;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimatorContextTest {
    @Test
    void shouldResolveScalarAndVectorParameters() {
        AnimatorContext context = AnimatorContext.builder()
                .distanceToCamera(0f)
                .visible(true)
                .forceTick(false)
                .scalarParameter("speed", 0.75f)
                .vectorParameter("left_foot_target", new Vec3f(1f, -2f, 3f))
                .build();

        assertEquals(0.75f, context.scalar("speed", 0f), 0.0001f);
        assertEquals(new Vec3f(1f, -2f, 3f), context.vector("left_foot_target", Vec3f.ZERO));
    }

    @Test
    void shouldKeepBuilderRoundtrip() {
        AnimatorContext base = AnimatorContext.builder()
                .distanceToCamera(24f)
                .visible(false)
                .forceTick(true)
                .scalarParameters(Map.of("a", 1f))
                .vectorParameters(Map.of("b", new Vec3f(2f, 3f, 4f)))
                .build();

        AnimatorContext rebuilt = base.toBuilder()
                .scalarParameter("c", 2f)
                .build();

        assertEquals(24f, rebuilt.distanceToCamera(), 0.0001f);
        assertEquals(false, rebuilt.visible());
        assertEquals(true, rebuilt.forceTick());
        assertEquals(1f, rebuilt.scalar("a", 0f), 0.0001f);
        assertEquals(2f, rebuilt.scalar("c", 0f), 0.0001f);
        assertEquals(new Vec3f(2f, 3f, 4f), rebuilt.vector("b", Vec3f.ZERO));
    }
}
