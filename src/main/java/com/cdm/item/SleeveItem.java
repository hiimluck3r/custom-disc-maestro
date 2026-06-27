package com.cdm.item;

import java.util.List;

import com.cdm.data.SleeveDesign;
import com.cdm.menu.SleeveMenu;
import com.cdm.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A record sleeve. Shows its title and whether a record is tucked inside, so packaging is visible. */
public class SleeveItem extends Item {
    public SleeveItem(Properties properties) {
        super(properties);
    }

    /** Right-click in hand opens a one-slot menu to drop a record into the sleeve (no table needed). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sleeve = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new SleeveMenu(id, inv, hand), sleeve.getHoverName());
            serverPlayer.openMenu(provider, buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(sleeve, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SleeveDesign design = stack.get(ModComponents.SLEEVE_DESIGN.get());
        if (design != null && !design.title().isBlank()) {
            tooltip.add(Component.literal(design.title()).withStyle(ChatFormatting.WHITE));
        }
        ItemStack contained = com.cdm.recipe.SleeveOps.containedRecord(stack);
        if (!contained.isEmpty()) {
            tooltip.add(Component.translatable("cdm.sleeve.contains", contained.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("cdm.sleeve.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
