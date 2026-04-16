package io.hoyatla.animatruc.forge.ground;

import io.hoyatla.animatruc.core.math.Vec3f;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateful ground-contact solver that computes local IK targets per limb from block raycasts.
 * Intended for client-side visual animation only.
 */
public final class GroundContactController {
    private final List<GroundContactLimbDefinition> limbs;
    private final Map<String, LimbState> states = new HashMap<>();
    private final float baseSwingDurationTicks;
    private final float minHorizontalSpeed;
    private final float gaitFrequency;

    public GroundContactController(
            List<GroundContactLimbDefinition> limbs,
            float baseSwingDurationTicks,
            float minHorizontalSpeed,
            float gaitFrequency) {
        if (limbs == null || limbs.isEmpty())
            throw new IllegalArgumentException("limbs must not be empty");

        this.limbs = List.copyOf(limbs);
        this.baseSwingDurationTicks = Math.max(0.1f, baseSwingDurationTicks);
        this.minHorizontalSpeed = Math.max(0f, minHorizontalSpeed);
        this.gaitFrequency = Math.max(0.001f, gaitFrequency);
    }

    public static GroundContactController forHumanoid(
            GroundContactLimbDefinition leftLeg,
            GroundContactLimbDefinition rightLeg) {
        return new GroundContactController(List.of(leftLeg, rightLeg), 5f, 0.02f, 0.12f);
    }

    public Map<String, Vec3f> update(Entity entity, float bodyYawDegrees, float deltaTicks) {
        if (entity == null)
            return Map.of();

        float safeDelta = Math.max(0f, deltaTicks);
        Vec3 velocity = entity.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double swingDuration = this.baseSwingDurationTicks / Math.max(0.25d, horizontalSpeed * 8d);
        double tickClock = entity.tickCount + safeDelta;
        Map<String, Vec3f> targets = new HashMap<>(this.limbs.size());

        for (GroundContactLimbDefinition limb : this.limbs) {
            LimbState state = this.states.computeIfAbsent(limb.id(), key -> new LimbState());
            Vec3 anchorWorld = entity.position().add(localToWorldOffset(limb.anchorLocal(), bodyYawDegrees));
            Vec3 desiredSample = desiredStepSample(anchorWorld, bodyYawDegrees, limb, tickClock, horizontalSpeed);
            Vec3 desiredGround = raycastGround(entity, desiredSample, limb.probeUpDistance(), limb.probeDownDistance());

            if (!state.initialized) {
                state.current = desiredGround;
                state.planted = desiredGround;
                state.initialized = true;
            }

            if (horizontalSpeed > this.minHorizontalSpeed) {
                tryStartSwing(state, desiredGround, limb.stepTriggerDistance());
                advanceSwing(state, desiredGround, limb.liftHeight(), safeDelta, (float)swingDuration);
            } else if (!state.swinging) {
                state.current = state.planted == null ? desiredGround : state.planted;
            } else {
                advanceSwing(state, desiredGround, limb.liftHeight(), safeDelta, (float)swingDuration);
            }

            Vec3 localTarget = worldToLocal(entity.position(), state.current, bodyYawDegrees);
            targets.put(
                    limb.targetKey(),
                    new Vec3f((float)localTarget.x, (float)localTarget.y, (float)localTarget.z)
            );
        }

        return Map.copyOf(targets);
    }

    public void reset() {
        this.states.clear();
    }

    private Vec3 desiredStepSample(
            Vec3 anchorWorld,
            float bodyYawDegrees,
            GroundContactLimbDefinition limb,
            double tickClock,
            double horizontalSpeed) {
        double phase = (tickClock * this.gaitFrequency + limb.phaseOffset()) * (Math.PI * 2d);
        double stride = Math.sin(phase) * limb.stepDistance() * Math.min(1d, horizontalSpeed * 6d);
        Vec3 forward = forwardFromYaw(bodyYawDegrees);

        return anchorWorld.add(forward.scale(stride));
    }

    private static void tryStartSwing(LimbState state, Vec3 desiredGround, float triggerDistance) {
        if (state.swinging || state.planted == null)
            return;

        double horizontalDistance = horizontalDistance(state.planted, desiredGround);

        if (horizontalDistance < triggerDistance)
            return;

        state.swinging = true;
        state.swingProgress = 0f;
        state.swingStart = state.current;
        state.swingEnd = desiredGround;
    }

    private static void advanceSwing(
            LimbState state,
            Vec3 desiredGround,
            float liftHeight,
            float deltaTicks,
            float swingDurationTicks) {
        if (!state.swinging) {
            state.planted = desiredGround;
            state.current = desiredGround;
            return;
        }

        state.swingEnd = desiredGround;
        float duration = Math.max(0.1f, swingDurationTicks);
        state.swingProgress = Math.min(1f, state.swingProgress + deltaTicks / duration);
        float t = state.swingProgress;
        double lift = Math.sin(Math.PI * t) * liftHeight;
        Vec3 base = lerp(state.swingStart, state.swingEnd, t);
        state.current = new Vec3(base.x, base.y + lift, base.z);

        if (t >= 1f) {
            state.swinging = false;
            state.current = state.swingEnd;
            state.planted = state.swingEnd;
        }
    }

    private static Vec3 raycastGround(Entity entity, Vec3 sample, float probeUp, float probeDown) {
        Vec3 from = sample.add(0d, probeUp, 0d);
        Vec3 to = sample.add(0d, -probeDown, 0d);
        BlockHitResult hit = entity.level().clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS)
            return sample.add(0d, -probeDown, 0d);

        return hit.getLocation();
    }

    private static Vec3 localToWorldOffset(Vec3f local, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = local.x() * cos - local.z() * sin;
        double z = local.x() * sin + local.z() * cos;
        return new Vec3(x, local.y(), z);
    }

    private static Vec3 worldToLocal(Vec3 entityPos, Vec3 worldTarget, float yawDegrees) {
        Vec3 relative = worldTarget.subtract(entityPos);
        double yaw = Math.toRadians(yawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double x = relative.x * cos + relative.z * sin;
        double z = -relative.x * sin + relative.z * cos;
        return new Vec3(x, relative.y, z);
    }

    private static Vec3 forwardFromYaw(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(-sin, 0d, cos);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, float t) {
        double alpha = Math.max(0d, Math.min(1d, t));
        return new Vec3(
                a.x + (b.x - a.x) * alpha,
                a.y + (b.y - a.y) * alpha,
                a.z + (b.z - a.z) * alpha
        );
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static final class LimbState {
        private boolean initialized;
        private boolean swinging;
        private float swingProgress;
        private Vec3 planted = Vec3.ZERO;
        private Vec3 current = Vec3.ZERO;
        private Vec3 swingStart = Vec3.ZERO;
        private Vec3 swingEnd = Vec3.ZERO;
    }
}
