package com.cdm.registry;

import java.util.function.Supplier;

import com.cdm.CDMMod;
import com.cdm.menu.CuttingLatheMenu;
import com.cdm.menu.PackagingMenu;
import com.cdm.menu.RecordPressMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CDMMod.MODID);

    public static final Supplier<MenuType<PackagingMenu>> PACKAGING =
            MENUS.register("packaging", () -> IMenuTypeExtension.create(PackagingMenu::new));

    public static final Supplier<MenuType<CuttingLatheMenu>> CUTTING_LATHE =
            MENUS.register("cutting_lathe", () -> IMenuTypeExtension.create(CuttingLatheMenu::new));

    public static final Supplier<MenuType<RecordPressMenu>> RECORD_PRESS =
            MENUS.register("record_press", () -> IMenuTypeExtension.create(RecordPressMenu::new));
}
