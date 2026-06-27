package com.cdm;

import org.slf4j.Logger;

import com.cdm.registry.ModBlockEntities;
import com.cdm.registry.ModBlocks;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModFluids;
import com.cdm.registry.ModMenus;
import com.cdm.registry.ModRecipes;
import com.cdm.registry.ModCreativeTabs;
import com.cdm.registry.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Main entry point for the Custom Disc Maestro mod. Wires the deferred registers to the mod event bus.
 */
@Mod(CDMMod.MODID)
public class CDMMod {
    public static final String MODID = "cdm";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CDMMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModComponents.COMPONENTS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CDMConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(com.cdm.cauldron.NickelCauldron::setup);
        LOGGER.info("Custom Disc Maestro: common setup complete.");
    }
}
