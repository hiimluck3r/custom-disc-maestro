package com.cdm.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The galvanic matrix. Carries a track's {@code RecordContent} + {@code DiscMeta}, so it shows the song
 * name and author/album exactly like the music disc it was grown from. Has durability for a run of presses.
 */
public class MatrixItem extends Item {
    public MatrixItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return DiscNaming.name(stack, () -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DiscNaming.appendMeta(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
