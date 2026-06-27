package com.cdm.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The galvanic matrix. Carries a track's {@code RecordContent} + {@code DiscMeta}. It keeps its own item
 * name ("Galvanic Matrix") and shows the track title + author/album in the tooltip. Has durability for a
 * run of presses.
 */
public class MatrixItem extends Item {
    public MatrixItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DiscNaming.appendTitledMeta(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
