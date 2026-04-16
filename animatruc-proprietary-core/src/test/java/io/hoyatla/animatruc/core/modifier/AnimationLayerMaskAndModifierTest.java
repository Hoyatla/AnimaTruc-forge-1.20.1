package io.hoyatla.animatruc.core.modifier;

import io.hoyatla.animatruc.core.animation.AnimationLayer;
import io.hoyatla.animatruc.core.animation.BoneMask;
import io.hoyatla.animatruc.core.animation.ClipState;
import io.hoyatla.animatruc.core.math.Quatf;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AdaptiveUpdatePolicy;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;
import io.hoyatla.animatruc.core.runtime.AnimatorInstance;
import io.hoyatla.animatruc.core.testing.TestClipFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationLayerMaskAndModifierTest {
    @Test
    void shouldApplyBoneMask() {
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        BoneMask mask = new BoneMask.Builder()
                .defaultWeight(0f)
                .include("body")
                .exclude("hand")
                .build();

        animator.play(
                AnimationLayer.of(new ClipState(
                        TestClipFactory.multiBoneConstant(
                                "multi",
                                Map.of("body", new Vec3f(5f, 0f, 0f), "hand", new Vec3f(3f, 0f, 0f))
                        ),
                        1f,
                        false
                )).withMask(mask)
        );

        var result = animator.update(AnimatorContext.visibleNear(), 1f).pose();
        assertTrue(result.transform("body").translation().x() > 4f);
        assertEquals(0f, result.transform("hand").translation().x(), 0.0001f);
    }

    @Test
    void shouldApplyLookAtAndBreathingModifiers() {
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        animator.play(new ClipState(TestClipFactory.singleBoneConstant("idle", "head", Vec3f.ZERO), 1f, false));
        animator.addModifier(new LookAtModifier("head", 1f, 1f, 90f, 90f, 0.5f));
        animator.addModifier(new BreathingModifier("head", 0.2f, 0f, 0.5f));

        AnimatorContext context = AnimatorContext.builder()
                .distanceToCamera(0f)
                .visible(true)
                .forceTick(false)
                .lookYawDegrees(35f)
                .lookPitchDegrees(-15f)
                .build();

        var pose = animator.update(context, 1f).pose();
        assertNotEquals(Quatf.IDENTITY, pose.transform("head").rotation());
        assertNotEquals(0f, pose.transform("head").translation().y(), 0.0001f);
    }

    @Test
    void shouldApplyTwoBoneIkModifier() {
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        animator.play(new ClipState(
                TestClipFactory.multiBoneConstant(
                        "ik",
                        Map.of("upper_arm", Vec3f.ZERO, "lower_arm", Vec3f.ZERO, "hand", Vec3f.ZERO)
                ),
                1f,
                false
        ));
        animator.addModifier(new TwoBoneIKModifier("upper_arm", "lower_arm", "hand", 4f, 4f, 1f));

        AnimatorContext context = AnimatorContext.builder()
                .distanceToCamera(0f)
                .visible(true)
                .forceTick(false)
                .ikTarget(new Vec3f(5f, 2f, 0f))
                .build();

        var pose = animator.update(context, 1f).pose();
        assertNotEquals(Quatf.IDENTITY, pose.transform("upper_arm").rotation());
        assertNotEquals(Quatf.IDENTITY, pose.transform("lower_arm").rotation());
    }

    @Test
    void shouldApplyMultiLimbIkTargetsPerChain() {
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        animator.play(new ClipState(
                TestClipFactory.multiBoneConstant(
                        "walk",
                        Map.of(
                                "left_upper_leg", Vec3f.ZERO,
                                "left_lower_leg", Vec3f.ZERO,
                                "left_foot", Vec3f.ZERO,
                                "right_upper_leg", Vec3f.ZERO,
                                "right_lower_leg", Vec3f.ZERO,
                                "right_foot", Vec3f.ZERO
                        )
                ),
                1f,
                false
        ));

        animator.addModifier(new MultiLimbIKModifier(
                List.of(
                        IkChainDefinition.builder(
                                "left_leg",
                                "left_upper_leg",
                                "left_lower_leg",
                                "left_foot",
                                "ik_left_foot"
                        ).plane(IkPlane.YZ).lengths(5f, 5f).build(),
                        IkChainDefinition.builder(
                                "right_leg",
                                "right_upper_leg",
                                "right_lower_leg",
                                "right_foot",
                                "ik_right_foot"
                        ).plane(IkPlane.YZ).lengths(5f, 5f).build()
                ),
                false
        ));

        AnimatorContext context = AnimatorContext.builder()
                .distanceToCamera(0f)
                .visible(true)
                .forceTick(false)
                .vectorParameters(Map.of(
                        "ik_left_foot", new Vec3f(0.1f, -3f, 2f),
                        "ik_right_foot", new Vec3f(-0.1f, -3f, -2f)
                ))
                .build();

        var pose = animator.update(context, 1f).pose();
        Quatf leftRoot = pose.transform("left_upper_leg").rotation();
        Quatf rightRoot = pose.transform("right_upper_leg").rotation();

        assertNotEquals(Quatf.IDENTITY, leftRoot);
        assertNotEquals(Quatf.IDENTITY, rightRoot);
        assertNotEquals(leftRoot, rightRoot);
    }
}
