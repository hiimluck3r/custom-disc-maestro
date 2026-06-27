package com.cdm.block;

import com.cdm.cauldron.NickelCauldron;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/**
 * A cauldron filled with the emerald nickel bath ("the well"). Reuses vanilla cauldron levels (1-3);
 * its interactions ({@link NickelCauldron#INTERACTIONS}) scoop nickel into a bucket and electroform a
 * recorded disc into a galvanic matrix. Never fills from precipitation.
 */
public class NickelCauldronBlock extends LayeredCauldronBlock {
    public NickelCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, NickelCauldron.INTERACTIONS, properties);
    }
}
