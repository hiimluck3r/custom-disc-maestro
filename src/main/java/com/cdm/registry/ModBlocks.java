package com.cdm.registry;

import com.cdm.CDMMod;
import com.cdm.block.CuttingLatheBlock;
import com.cdm.block.NickelCauldronBlock;
import com.cdm.block.NickelLiquidBlock;
import com.cdm.block.PackagingTableBlock;
import com.cdm.block.RecordPressBlock;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Blocks for the vinyl workflow: the three worktables (lathe, press, packaging) plus the kupfernickel
 * fluid and its cauldron. Playback stays in the vanilla jukebox — no bespoke block for it.
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CDMMod.MODID);

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties appliance() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(2.0f)
                .sound(SoundType.WOOD);
    }

    // Compose a disc here: note sequencer + .nbs import + author/title/album metadata.
    public static final DeferredBlock<CuttingLatheBlock> CUTTING_LATHE =
            BLOCKS.registerBlock("cutting_lathe", CuttingLatheBlock::new, machine());

    // Sleeve design + stencil baking (records are sleeved by right-clicking the sleeve).
    public static final DeferredBlock<PackagingTableBlock> PACKAGING_TABLE =
            BLOCKS.registerBlock("packaging_table", PackagingTableBlock::new, appliance());

    // Press a designed disc from a galvanic matrix + blank (where vinyl colour/label are applied).
    public static final DeferredBlock<RecordPressBlock> RECORD_PRESS =
            BLOCKS.registerBlock("record_press", RecordPressBlock::new, machine());

    // The placed nickel bath fluid (Wither on contact).
    public static final DeferredBlock<NickelLiquidBlock> NICKEL_LIQUID = BLOCKS.registerBlock("kupfernickel",
            props -> new NickelLiquidBlock(ModFluids.NICKEL.get(), props),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GLOW_LICHEN)
                    .replaceable()
                    .noCollission()
                    .strength(100.0f)
                    .noLootTable()
                    .liquid()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.EMPTY));

    // The "well": a cauldron holding the nickel bath (scoop with a bucket; dip a disc to make a matrix).
    public static final DeferredBlock<NickelCauldronBlock> NICKEL_CAULDRON = BLOCKS.registerBlock("kupfernickel_cauldron",
            NickelCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));
}
