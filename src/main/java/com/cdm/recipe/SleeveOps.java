package com.cdm.recipe;

import java.util.List;

import com.cdm.data.SleeveDesign;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Shared predicates/operations for the sleeve crafting recipes. */
public final class SleeveOps {
    private SleeveOps() {}

    public static boolean isSleeve(ItemStack stack) {
        return stack.is(ModItems.SLEEVE.get()) || stack.is(ModItems.SLEEVE_BLANK.get());
    }

    /**
     * Any music disc — our custom disc or a vanilla one — can be sleeved. In 1.21.1 a "music disc" is
     * identified by the {@code jukebox_playable} component (there is no music_discs item tag).
     */
    public static boolean isRecord(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    public static boolean hasContainedRecord(ItemStack sleeve) {
        return !containedRecord(sleeve).isEmpty();
    }

    // The contained disc is stored via vanilla's ItemContainerContents (a raw ItemStack can't be a data
    // component value — it has no equals/hashCode, which NeoForge rejects on set).
    public static ItemStack containedRecord(ItemStack sleeve) {
        ItemContainerContents contents = sleeve.get(ModComponents.CONTAINED_RECORD.get());
        return contents == null ? ItemStack.EMPTY : contents.getSlots() == 0 ? ItemStack.EMPTY
                : contents.getStackInSlot(0).copy();
    }

    public static void setContainedRecord(ItemStack sleeve, ItemStack record) {
        if (record == null || record.isEmpty()) {
            sleeve.remove(ModComponents.CONTAINED_RECORD.get());
        } else {
            sleeve.set(ModComponents.CONTAINED_RECORD.get(),
                    ItemContainerContents.fromItems(List.of(record.copyWithCount(1))));
        }
    }

    public static ItemStack emptiedSleeve(ItemStack sleeve) {
        ItemStack copy = sleeve.copyWithCount(1);
        copy.remove(ModComponents.CONTAINED_RECORD.get());
        return copy;
    }

    public static SleeveDesign design(ItemStack stack) {
        return stack.get(ModComponents.SLEEVE_DESIGN.get());
    }

    /** Two short lines showing which pattern + colours a design uses, so templates are distinguishable. */
    public static void appendDesignTooltip(SleeveDesign d, List<Component> tooltip) {
        if (d == null) return;
        tooltip.add(Component.translatable("cdm.sleeve.pattern",
                Component.translatable("cdm.sleeve.pattern." + d.sticker())).withStyle(ChatFormatting.GRAY));
        Component cover = Component.literal("■").withStyle(s -> s.withColor(TextColor.fromRgb(d.bgColor())));
        Component sticker = Component.literal("■").withStyle(s -> s.withColor(TextColor.fromRgb(d.stickerColor())));
        tooltip.add(Component.translatable("cdm.sleeve.colors", cover, sticker).withStyle(ChatFormatting.GRAY));
    }
}
