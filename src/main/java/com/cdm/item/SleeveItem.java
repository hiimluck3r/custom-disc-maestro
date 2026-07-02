package com.cdm.item;

import java.util.List;

import com.cdm.data.SleeveDesign;
import com.cdm.recipe.SleeveOps;
import com.cdm.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A record sleeve with vanilla-bundle interactions: in any inventory, right-click the sleeve onto a
 * record (or a record onto the sleeve) to tuck it in, right-click the filled sleeve onto an empty
 * slot to slide it back out, and use the filled sleeve in hand to toss the record on the ground.
 * One record per sleeve; interactions apply to single sleeves only (like a bundle).
 */
public class SleeveItem extends Item {
    public SleeveItem(Properties properties) {
        super(properties);
    }

    /** The sleeve rides the cursor and is right-clicked onto {@code slot}. */
    @Override
    public boolean overrideStackedOnOther(ItemStack sleeve, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || sleeve.getCount() != 1) {
            return false;
        }
        ItemStack slotItem = slot.getItem();
        if (slotItem.isEmpty()) {
            ItemStack inside = SleeveOps.containedRecord(sleeve);
            if (!inside.isEmpty() && slot.safeInsert(inside).isEmpty()) {
                SleeveOps.setContainedRecord(sleeve, ItemStack.EMPTY);
                playRemoveSound(player);
                return true;
            }
        } else if (SleeveOps.isRecord(slotItem) && !SleeveOps.hasContainedRecord(sleeve)) {
            ItemStack taken = slot.safeTake(slotItem.getCount(), 1, player);
            if (!taken.isEmpty()) {
                SleeveOps.setContainedRecord(sleeve, taken);
                playInsertSound(player);
                return true;
            }
        }
        return false;
    }

    /** {@code other} rides the cursor and is right-clicked onto the sleeve sitting in {@code slot}. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack sleeve, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || sleeve.getCount() != 1 || !slot.allowModification(player)) {
            return false;
        }
        if (other.isEmpty()) {
            ItemStack inside = SleeveOps.containedRecord(sleeve);
            if (!inside.isEmpty()) {
                SleeveOps.setContainedRecord(sleeve, ItemStack.EMPTY);
                access.set(inside);
                playRemoveSound(player);
                return true;
            }
        } else if (SleeveOps.isRecord(other) && !SleeveOps.hasContainedRecord(sleeve)) {
            SleeveOps.setContainedRecord(sleeve, other.copyWithCount(1));
            other.shrink(1);
            playInsertSound(player);
            return true;
        }
        return false;
    }

    /** Using a filled sleeve tosses its record onto the ground, like emptying a bundle. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack sleeve = player.getItemInHand(hand);
        if (sleeve.getCount() != 1) {
            return InteractionResultHolder.pass(sleeve);
        }
        ItemStack inside = SleeveOps.containedRecord(sleeve);
        if (inside.isEmpty()) {
            return InteractionResultHolder.pass(sleeve);
        }
        SleeveOps.setContainedRecord(sleeve, ItemStack.EMPTY);
        if (!level.isClientSide) {
            player.drop(inside, true);
        }
        playDropSound(player);
        return InteractionResultHolder.sidedSuccess(sleeve, level.isClientSide());
    }

    private static void playInsertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    private static void playRemoveSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    private static void playDropSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SleeveDesign design = stack.get(ModComponents.SLEEVE_DESIGN.get());
        if (design != null && !design.title().isBlank()) {
            tooltip.add(Component.literal(design.title()).withStyle(ChatFormatting.WHITE));
        }
        ItemStack contained = SleeveOps.containedRecord(stack);
        if (!contained.isEmpty()) {
            tooltip.add(Component.translatable("cdm.sleeve.contains", contained.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("cdm.sleeve.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
