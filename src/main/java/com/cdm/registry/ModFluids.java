package com.cdm.registry;

import com.cdm.CDMMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The "nickel" plating bath: a real, placeable fluid. Brewed in a water cauldron from copper + a spider
 * eye (the name comes from <i>Kupfernickel</i>); standing in it inflicts Wither, and dropping a recorded
 * master into it grows a galvanic {@link ModItems#MATRIX}.
 */
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CDMMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CDMMod.MODID);

    public static final DeferredHolder<FluidType, FluidType> NICKEL_TYPE = FLUID_TYPES.register("kupfernickel",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.cdm.kupfernickel")
                    .density(2400)
                    .viscosity(2400)
                    .canConvertToSource(false)
                    .canSwim(false)
                    .canDrown(true)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> NICKEL =
            FLUIDS.register("kupfernickel", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> NICKEL_FLOWING =
            FLUIDS.register("kupfernickel_flowing", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties;

    // Built lazily (when the first fluid is created) to avoid a static forward reference. FLUID registers
    // before BLOCK/ITEM, so the block/bucket holders resolve by the time this runs.
    private static BaseFlowingFluid.Properties properties() {
        if (properties == null) {
            properties = new BaseFlowingFluid.Properties(NICKEL_TYPE, NICKEL, NICKEL_FLOWING)
                    .block(ModBlocks.NICKEL_LIQUID)
                    .bucket(ModItems.NICKEL_BUCKET)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(25);
        }
        return properties;
    }
}
