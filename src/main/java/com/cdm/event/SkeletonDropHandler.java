package com.cdm.event;

import com.cdm.CDMMod;
import com.cdm.item.Patterns;
import com.cdm.registry.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** When a skeleton kills another skeleton, the victim drops a random pattern template (100%). */
@EventBusSubscriber(modid = CDMMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SkeletonDropHandler {
    private SkeletonDropHandler() {}

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractSkeleton victim) || victim.level().isClientSide) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof AbstractSkeleton)) {
            return;
        }
        RandomSource random = victim.getRandom();
        Item template = switch (random.nextInt(Patterns.COUNT)) {
            case 0 -> ModItems.PATTERN_STRIPES.get();
            case 1 -> ModItems.PATTERN_RIBBON.get();
            default -> ModItems.PATTERN_DOTS.get();
        };
        victim.spawnAtLocation(new ItemStack(template));
    }
}
