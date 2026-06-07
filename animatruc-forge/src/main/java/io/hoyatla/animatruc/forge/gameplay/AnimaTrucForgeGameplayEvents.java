package io.hoyatla.animatruc.forge.gameplay;

import io.hoyatla.animatruc.core.gameplay.CameraFeedback;
import io.hoyatla.animatruc.core.gameplay.ExplosionStimulusEvent;
import io.hoyatla.animatruc.core.gameplay.GameplayTickResult;
import io.hoyatla.animatruc.core.gameplay.TickGameplayEvent;
import io.hoyatla.animatruc.core.gameplay.WeightUpdateEvent;
import io.hoyatla.animatruc.core.math.Vec3f;
import io.hoyatla.animatruc.forge.config.AnimaTrucGameplayConfig;
import io.hoyatla.animatruc.forge.network.AnimaTrucForgeNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class AnimaTrucForgeGameplayEvents {
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (!AnimaTrucGameplayConfig.ENABLE_EXPLOSION_FEEDBACK.get())
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        Explosion explosion = event.getExplosion();
        double maxDistance = AnimaTrucGameplayConfig.EXPLOSION_FEEDBACK_RANGE.get();
        double maxDistanceSqr = maxDistance * maxDistance;
        int affectedBlocks = event.getAffectedBlocks().size();
        float estimatedPower = Math.max(1f, (float)Math.sqrt(affectedBlocks + 1) / 2.5f);
        Vec3f position = new Vec3f((float)explosion.getPosition().x, (float)explosion.getPosition().y, (float)explosion.getPosition().z);

        for (ServerPlayer player : level.players()) {
            double distanceSqr = player.distanceToSqr(explosion.getPosition());
            if (distanceSqr > maxDistanceSqr)
                continue;

            String actorId = player.getStringUUID();
            GameplayTickResult result = AnimaTrucForgeGameplayRuntime.dispatch(new ExplosionStimulusEvent(
                    actorId,
                    position,
                    (float)Math.sqrt(distanceSqr),
                    estimatedPower,
                    affectedBlocks
            ));
            CameraFeedback feedback = result.mergedCameraFeedback(actorId);
            AnimaTrucForgeNetwork.sendFeedback(player, feedback);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide())
            return;
        if (!(event.player instanceof ServerPlayer player))
            return;

        String actorId = player.getStringUUID();
        AnimaTrucForgeGameplayRuntime.dispatch(new TickGameplayEvent(actorId, 1f));

        if (!AnimaTrucGameplayConfig.ENABLE_WEIGHT.get() || player.tickCount % 20 != 0)
            return;

        float carriedWeight = estimateInventoryWeight(player.getInventory());
        AnimaTrucForgeGameplayRuntime.dispatch(new WeightUpdateEvent(
                actorId,
                carriedWeight,
                AnimaTrucGameplayConfig.WEIGHT_MAX_COMFORT.get().floatValue()
        ));
    }

    private static float estimateInventoryWeight(Inventory inventory) {
        float weight = 0f;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty())
                continue;

            float stackFactor = stack.getCount() / (float)Math.max(1, stack.getMaxStackSize());
            float itemWeight = stack.getMaxStackSize() <= 1 ? 4f : 1f;
            weight += itemWeight * stackFactor;
        }
        return weight;
    }
}
