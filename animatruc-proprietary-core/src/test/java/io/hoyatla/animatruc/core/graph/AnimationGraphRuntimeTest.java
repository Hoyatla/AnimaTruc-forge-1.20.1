package io.hoyatla.animatruc.core.graph;

import io.hoyatla.animatruc.core.animation.AnimationClip;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AdaptiveUpdatePolicy;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;
import io.hoyatla.animatruc.core.runtime.AnimatorInstance;
import io.hoyatla.animatruc.core.testing.TestClipFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationGraphRuntimeTest {
    @Test
    void shouldTransitionBetweenStatesWithFade() {
        AnimationClip idle = TestClipFactory.singleBoneConstant("idle", "body", new Vec3f(0f, 0f, 0f));
        AnimationClip run = TestClipFactory.singleBoneConstant("run", "body", new Vec3f(10f, 0f, 0f));

        AnimationGraph graph = AnimationGraph.builder("idle")
                .state(AnimationGraphState.of("idle", idle))
                .state(AnimationGraphState.of("run", run))
                .transition(AnimationGraphTransition.of("idle", "run", (params, ctx) -> params.getFloat("speed", 0f) > 0.1f).withFadeTicks(5f))
                .transition(AnimationGraphTransition.of("run", "idle", (params, ctx) -> params.getFloat("speed", 0f) < 0.1f).withFadeTicks(5f))
                .build();

        AnimationGraphRuntime graphRuntime = new AnimationGraphRuntime(graph);
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        animator.setGraphRuntime(graphRuntime);

        AnimatorContext context = AnimatorContext.visibleNear();

        float idleX = animator.update(context, 1f).pose().transform("body").translation().x();
        assertTrue(idleX < 0.001f);

        graphRuntime.parameters().setFloat("speed", 1f);

        for (int i = 0; i < 6; i++) {
            animator.update(context, 1f);
        }

        float runX = animator.update(context, 1f).pose().transform("body").translation().x();
        assertTrue(runX > 8f);

        graphRuntime.parameters().setFloat("speed", 0f);

        for (int i = 0; i < 6; i++) {
            animator.update(context, 1f);
        }

        float backToIdleX = animator.update(context, 1f).pose().transform("body").translation().x();
        assertTrue(backToIdleX < 2f);
    }
}
