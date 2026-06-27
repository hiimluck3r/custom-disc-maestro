package com.cdm.event;

import java.util.List;

import com.cdm.CDMMod;
import com.cdm.cauldron.NickelCauldron;
import com.cdm.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Brew the nickel bath by THROWING the reagents in: a copper ingot and a spider eye dropped into a water
 * cauldron are consumed and the cauldron becomes a nickel bath (emerald bubbles). Kupfernickel, literally.
 */
@EventBusSubscriber(modid = CDMMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class NickelBrewingHandler {
    private NickelBrewingHandler() {}

    @SubscribeEvent
    static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item)) {
            return;
        }
        Level level = item.level();
        if (level.isClientSide) {
            return;
        }
        ItemStack stack = item.getItem();
        boolean copper = stack.is(Items.COPPER_INGOT);
        boolean eye = stack.is(Items.SPIDER_EYE);
        if (!copper && !eye) {
            return;
        }
        BlockPos pos = item.blockPosition();
        if (!level.getBlockState(pos).is(Blocks.WATER_CAULDRON)) {
            return;
        }
        Item needed = copper ? Items.SPIDER_EYE : Items.COPPER_INGOT;
        List<ItemEntity> partners = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos),
                other -> other != item && other.isAlive() && other.getItem().is(needed));
        if (partners.isEmpty()) {
            return;
        }
        ItemEntity partner = partners.get(0);
        consume(item);
        consume(partner);
        level.setBlockAndUpdate(pos, ModBlocks.NICKEL_CAULDRON.get().defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3));
        NickelCauldron.brewBurst(level, pos);
        NickelCauldron.brewSound(level, pos);
    }

    private static void consume(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
    }
}
