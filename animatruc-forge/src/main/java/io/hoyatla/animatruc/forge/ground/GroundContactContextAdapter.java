package io.hoyatla.animatruc.forge.ground;

import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Small bridge utility to merge block-contact IK targets into an animator context.
 */
public final class GroundContactContextAdapter {
    private GroundContactContextAdapter() {
    }

    public static AnimatorContext attachTargets(
            AnimatorContext base,
            Map<String, Vec3f> targetVectors,
            float horizontalSpeed) {
        AnimatorContext.Builder builder = base == null ? AnimatorContext.builder() : base.toBuilder();

        builder.vectorParameters(targetVectors == null ? Map.of() : targetVectors);
        builder.scalarParameter("locomotion_speed", horizontalSpeed);
        return builder.build();
    }

    public static float horizontalSpeed(Entity entity) {
        if (entity == null)
            return 0f;

        Vec3 velocity = entity.getDeltaMovement();
        return (float)Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }
}
