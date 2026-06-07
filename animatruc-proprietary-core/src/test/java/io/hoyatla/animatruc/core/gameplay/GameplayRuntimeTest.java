package io.hoyatla.animatruc.core.gameplay;

import io.hoyatla.animatruc.core.math.Vec3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameplayRuntimeTest {
    @Test
    void locomotionActionCreatesAnimationIntentAndConsumesStamina() {
        GameplayRuntime runtime = GameplayRuntimeFactory.createStandardRuntime();

        GameplayTickResult result = runtime.dispatch(new ActionRequestEvent("player", GameplayAction.ROLL, true, 1f));

        assertFalse(result.animationIntents().isEmpty());
        assertEquals("locomotion/roll", result.animationIntents().get(0).clipName());
        assertTrue(runtime.actor("player").stamina() < GameplayRuntimeConfig.DEFAULT.maxStamina());
    }

    @Test
    void soundStimulusRaisesAlertAndEmitsInvestigateAnimation() {
        GameplayRuntime runtime = GameplayRuntimeFactory.createStandardRuntime();

        GameplayTickResult result = runtime.dispatch(new SoundStimulusEvent("mob", "gunshot", Vec3f.ZERO, 0.8f, 0.7f, 8f));

        assertTrue(runtime.actor("mob").alertLevel() > 0f);
        assertTrue(result.animationIntents().stream().anyMatch(intent -> intent.clipName().startsWith("perception/")));
    }

    @Test
    void explosionStimulusProducesCameraFeedback() {
        GameplayRuntime runtime = GameplayRuntimeFactory.createStandardRuntime();

        GameplayTickResult result = runtime.dispatch(new ExplosionStimulusEvent("player", Vec3f.ZERO, 3f, 6f, 80));

        CameraFeedback feedback = result.mergedCameraFeedback("player");
        assertTrue(feedback.shake() > 0f);
        assertTrue(feedback.blurTicks() > 0);
    }

    @Test
    void weightRuntimeEmitsHeavyPosePastOrangeThreshold() {
        GameplayRuntime runtime = GameplayRuntimeFactory.createStandardRuntime();

        GameplayTickResult result = runtime.dispatch(new WeightUpdateEvent("player", 90f, 100f));

        assertEquals(0.9f, runtime.actor("player").weightRatio(), 0.0001f);
        assertTrue(result.animationIntents().stream().anyMatch(intent -> intent.clipName().equals("weight/heavy_pose")));
    }

    @Test
    void disabledFeatureDoesNotEmitIntents() {
        GameplayRuntimeConfig config = GameplayRuntimeConfig.builder().locomotionEnabled(false).build();
        GameplayRuntime runtime = GameplayRuntimeFactory.createStandardRuntime(config);

        GameplayTickResult result = runtime.dispatch(new ActionRequestEvent("player", GameplayAction.ROLL, true, 1f));

        assertTrue(result.emptyResult());
    }
}
