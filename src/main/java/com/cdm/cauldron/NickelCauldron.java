package com.cdm.cauldron;

import com.cdm.data.DiscMeta;
import com.cdm.data.RecordContent;
import com.cdm.registry.ModBlocks;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The "nickel bath" cauldron behaviour. A nickel-filled cauldron lets you scoop it back into a bucket,
 * or right-click it with a recorded disc to electroform a galvanic matrix for that track (the disc is
 * kept, the bath is spent a little and bubbles emerald particles). Pouring a nickel bucket into an empty
 * cauldron fills it (registered on {@link CauldronInteraction#EMPTY}).
 */
public final class NickelCauldron {
    private NickelCauldron() {}

    /** Interaction map used by {@code NickelCauldronBlock} (populated in {@link #setup()}). */
    public static final CauldronInteraction.InteractionMap INTERACTIONS =
            CauldronInteraction.newInteractionMap("kupfernickel");

    /** Call once on common setup (main thread). */
    public static void setup() {
        INTERACTIONS.map().put(Items.BUCKET, NickelCauldron::scoop);
        INTERACTIONS.map().put(ModItems.MUSIC_DISC.get(), NickelCauldron::electroform);
        // Pour a nickel bucket into an empty cauldron to make a nickel bath.
        CauldronInteraction.EMPTY.map().put(ModItems.NICKEL_BUCKET.get(), NickelCauldron::fillFromBucket);
    }

    private static ItemInteractionResult scoop(BlockState state, Level level, BlockPos pos, Player player,
                                               InteractionHand hand, ItemStack bucket) {
        if (!level.isClientSide) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(bucket, player,
                    new ItemStack(ModItems.NICKEL_BUCKET.get())));
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            player.awardStat(Stats.USE_CAULDRON);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult electroform(BlockState state, Level level, BlockPos pos, Player player,
                                                     InteractionHand hand, ItemStack disc) {
        RecordContent content = disc.get(ModComponents.RECORD_CONTENT.get());
        if (content == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // not a recorded disc
        }
        // Only a master cut may be electroformed — a pressed (final) vinyl record cannot.
        if (!Boolean.TRUE.equals(disc.get(ModComponents.MASTER.get()))) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("cdm.cauldron.not_master"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack matrix = new ItemStack(ModItems.MATRIX.get());
            matrix.set(ModComponents.RECORD_CONTENT.get(), content);
            DiscMeta meta = disc.get(ModComponents.DISC_META.get());
            if (meta != null) {
                matrix.set(ModComponents.DISC_META.get(), meta);
            }
            // Bake the master's groove wear in, so a worn/broken master makes a matrix that stamps
            // equally worn/broken records.
            if (disc.isDamageableItem() && disc.getDamageValue() > 0) {
                matrix.set(ModComponents.BAKED_WEAR.get(), disc.getDamageValue());
            }
            player.getInventory().placeItemBackInInventory(matrix); // disc itself is kept
            LayeredCauldronBlock.lowerFillLevel(state, level, pos); // the bath is spent a little
            bubble(level, pos);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.3F);
            player.awardStat(Stats.USE_CAULDRON);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult fillFromBucket(BlockState state, Level level, BlockPos pos, Player player,
                                                        InteractionHand hand, ItemStack nickelBucket) {
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, ModBlocks.NICKEL_CAULDRON.get().defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, 3));
            player.setItemInHand(hand, ItemUtils.createFilledResult(nickelBucket, player, new ItemStack(Items.BUCKET)));
            player.awardStat(Stats.USE_CAULDRON);
            bubble(level, pos);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Emerald bubbles rising above the bath (small, for a dip). */
    public static void bubble(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 14, 0.22, 0.18, 0.22, 0.0);
        }
    }

    /** A bigger celebratory burst when the bath first forms: emerald swirl, splash, rising sparkles. */
    public static void brewBurst(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.55, z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.2, z, 34, 0.28, 0.25, 0.28, 0.0);
            server.sendParticles(ParticleTypes.SPLASH, x, y + 0.25, z, 24, 0.22, 0.06, 0.22, 0.2);
            server.sendParticles(ParticleTypes.ENCHANT, x, y + 1.0, z, 20, 0.16, 0.3, 0.16, 0.5);
            server.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 6, 0.14, 0.2, 0.14, 0.02);
        }
    }

    /** Layered fizz/chime played when the bath forms. */
    public static void brewSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.9F, 0.8F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.2F);
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.6F, 1.4F);
    }
}
