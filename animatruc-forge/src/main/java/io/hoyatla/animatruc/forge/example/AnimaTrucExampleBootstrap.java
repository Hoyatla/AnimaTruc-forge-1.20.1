package io.hoyatla.animatruc.forge.example;

import io.hoyatla.animatruc.core.animation.ClipState;
import io.hoyatla.animatruc.core.runtime.AdaptiveUpdatePolicy;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;
import io.hoyatla.animatruc.core.runtime.AnimatorInstance;
import io.hoyatla.animatruc.forge.pack.AnimaTrucForgePackLoader;
import io.hoyatla.animatruc.forge.preset.AutoGroundContactPresetFactory;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * Minimal example bootstrap showing how to load and consume an AnimaTruc runtime pack.
 */
public final class AnimaTrucExampleBootstrap {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation EXAMPLE_PACK = Objects.requireNonNull(
            ResourceLocation.tryBuild("animatruc", "example_humanoid.animatrucpack.json")
    );

    private AnimaTrucExampleBootstrap() {
    }

    public static void initialize() {
        AnimaTrucForgePackLoader loader = new AnimaTrucForgePackLoader();
        var pack = loader.loadFromModResources(EXAMPLE_PACK);

        if (pack.clipsByName().isEmpty()) {
            LOGGER.warn("AnimaTruc example pack loaded without clips");
            return;
        }

        var firstClip = pack.clipsByName().values().iterator().next();
        AnimatorInstance animator = new AnimatorInstance(AdaptiveUpdatePolicy.DEFAULT);
        animator.play(new ClipState(firstClip, 1f, false));

        AutoGroundContactPresetFactory.detect(pack.skeleton()).ifPresent(autoPreset -> {
            animator.addModifier(autoPreset.createIkModifier());
            LOGGER.info(
                    "AnimaTruc auto preset detected: profile={}, chains={}, confidence={}",
                    autoPreset.profile(),
                    autoPreset.ikChains().size(),
                    autoPreset.detectionReport().averageConfidence()
            );
        });

        var frame = animator.update(AnimatorContext.visibleNear(), 1f);

        LOGGER.info(
                "AnimaTruc example ready: bones={}, cubes={}, clips={}, sampledBones={}",
                pack.skeleton().orderedBones().size(),
                pack.geometry().cubes().size(),
                pack.clipsByName().size(),
                frame.pose().bones().size()
        );
    }
}
