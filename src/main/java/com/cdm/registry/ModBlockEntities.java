package com.cdm.registry;

import java.util.function.Supplier;

import com.cdm.CDMMod;
import com.cdm.block.entity.CuttingLatheBlockEntity;
import com.cdm.block.entity.PackagingTableBlockEntity;
import com.cdm.block.entity.RecordPressBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CDMMod.MODID);

    public static final Supplier<BlockEntityType<PackagingTableBlockEntity>> PACKAGING_TABLE =
            BLOCK_ENTITIES.register("packaging_table", () -> BlockEntityType.Builder.of(
                    PackagingTableBlockEntity::new, ModBlocks.PACKAGING_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<CuttingLatheBlockEntity>> CUTTING_LATHE =
            BLOCK_ENTITIES.register("cutting_lathe", () -> BlockEntityType.Builder.of(
                    CuttingLatheBlockEntity::new, ModBlocks.CUTTING_LATHE.get()).build(null));

    public static final Supplier<BlockEntityType<RecordPressBlockEntity>> RECORD_PRESS =
            BLOCK_ENTITIES.register("record_press", () -> BlockEntityType.Builder.of(
                    RecordPressBlockEntity::new, ModBlocks.RECORD_PRESS.get()).build(null));
}
