package com.cdm.recipe;

import com.cdm.data.SleeveDesign;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;
import com.cdm.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Crafting: a reusable sleeve stencil + N blank sleeves -> N sleeves with the stencil's design.
 * The stencil is returned in the grid (like a tool), so one stencil mass-copies a design.
 */
public class SleeveStencilRecipe extends CustomRecipe {
    public SleeveStencilRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isStencil(ItemStack stack) {
        return stack.is(ModItems.SLEEVE_STENCIL.get()) && SleeveOps.design(stack) != null;
    }

    private static boolean isBlank(ItemStack stack) {
        return stack.is(ModItems.SLEEVE_BLANK.get());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean stencil = false;
        int blanks = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (isStencil(stack) && !stencil) {
                stencil = true;
            } else if (isBlank(stack)) {
                blanks++;
            } else {
                return false;
            }
        }
        return stencil && blanks >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        SleeveDesign design = null;
        int blanks = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isStencil(stack)) design = SleeveOps.design(stack);
            else if (isBlank(stack)) blanks++; // crafting consumes one per slot
        }
        if (design == null || blanks <= 0) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(ModItems.SLEEVE.get(), Math.min(blanks, 64));
        result.set(ModComponents.SLEEVE_DESIGN.get(), design);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            if (isStencil(input.getItem(i))) {
                remaining.set(i, input.getItem(i).copy()); // stencil is reusable
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SLEEVE_STENCIL_STAMP.get();
    }
}
