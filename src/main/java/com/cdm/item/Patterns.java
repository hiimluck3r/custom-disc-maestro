package com.cdm.item;

import com.cdm.registry.ModItems;

import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

/**
 * Pattern-template helpers shared by the Press and the Packaging Table. A pattern template maps to a
 * pattern index (1 = stripes, 2 = ribbon, 3 = dots), matching the disc style / sleeve sticker indices.
 * Index 0 means "no pattern" (plain). Also maps dye items to colours (colours are paid for in dyes).
 */
public final class Patterns {
    private Patterns() {}

    public static final int COUNT = 3; // stripes, ribbon, dots

    public static int indexOf(ItemStack stack) {
        if (stack.is(ModItems.PATTERN_STRIPES.get())) return 1;
        if (stack.is(ModItems.PATTERN_RIBBON.get())) return 2;
        if (stack.is(ModItems.PATTERN_DOTS.get())) return 3;
        return 0;
    }

    public static boolean isTemplate(ItemStack stack) {
        return indexOf(stack) != 0;
    }

    /** RGB colour of a dye item, or -1 if the stack is not a dye. */
    public static int dyeColor(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dye) {
            return dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
        }
        return -1;
    }

    public static boolean isDye(ItemStack stack) {
        return stack.getItem() instanceof DyeItem;
    }
}
