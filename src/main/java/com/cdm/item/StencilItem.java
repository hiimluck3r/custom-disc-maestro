package com.cdm.item;

import java.util.List;

import com.cdm.data.SleeveDesign;
import com.cdm.recipe.SleeveOps;
import com.cdm.registry.ModComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A baked sleeve stencil. Its tooltip lists the pattern + colours so different templates are distinguishable. */
public class StencilItem extends Item {
    public StencilItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SleeveDesign design = stack.get(ModComponents.SLEEVE_DESIGN.get());
        SleeveOps.appendDesignTooltip(design, tooltip);
    }
}
