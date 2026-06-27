package com.cdm.item;

import java.util.List;

import com.cdm.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A custom music disc whose displayed name and tooltip come from its {@link DiscMeta} (title/author/
 * album). It is {@code jukebox_playable}, so it plays in a vanilla jukebox (notes via the mixin).
 */
public class MusicDiscItem extends Item {
    public MusicDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return DiscNaming.name(stack, () -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Boolean.TRUE.equals(stack.get(ModComponents.MASTER.get()))) {
            tooltip.add(Component.translatable("item.cdm.music_disc.master").withStyle(ChatFormatting.AQUA));
        }
        DiscNaming.appendMeta(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
