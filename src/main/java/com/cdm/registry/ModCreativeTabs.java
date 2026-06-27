package com.cdm.registry;

import com.cdm.CDMMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CDMMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CDM_TAB = TABS.register("cdm_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cdm"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.MUSIC_DISC.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CUTTING_LATHE_ITEM.get());
                        output.accept(ModItems.RECORD_PRESS_ITEM.get());
                        output.accept(ModItems.PACKAGING_TABLE_ITEM.get());
                        output.accept(ModItems.BLANK_DISC.get());
                        output.accept(ModItems.MUSIC_DISC.get());
                        output.accept(ModItems.MATRIX.get());
                        output.accept(ModItems.NICKEL_BUCKET.get());
                        output.accept(ModItems.SLEEVE_BLANK.get());
                        output.accept(ModItems.SLEEVE.get());
                        output.accept(ModItems.SLEEVE_STENCIL.get());
                        output.accept(ModItems.PATTERN_STRIPES.get());
                        output.accept(ModItems.PATTERN_RIBBON.get());
                        output.accept(ModItems.PATTERN_DOTS.get());
                    }).build());
}
