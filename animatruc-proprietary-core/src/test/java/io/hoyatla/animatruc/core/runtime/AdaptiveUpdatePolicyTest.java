package io.hoyatla.animatruc.core.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptiveUpdatePolicyTest {
    @Test
    void shouldApplyDefaultIntervals() {
        AdaptiveUpdatePolicy policy = AdaptiveUpdatePolicy.DEFAULT;

        assertEquals(1, policy.intervalFor(4f, true, false));
        assertEquals(2, policy.intervalFor(32f, true, false));
        assertEquals(4, policy.intervalFor(72f, true, false));
        assertEquals(10, policy.intervalFor(128f, true, false));
        assertEquals(6, policy.intervalFor(1f, false, false));
    }

    @Test
    void shouldForceTickWhenRequested() {
        AdaptiveUpdatePolicy policy = AdaptiveUpdatePolicy.DEFAULT;

        assertEquals(1, policy.intervalFor(1000f, false, true));
    }
}
