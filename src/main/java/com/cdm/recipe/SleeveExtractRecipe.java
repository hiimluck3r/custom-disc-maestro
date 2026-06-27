package com.cdm.recipe;

import com.cdm.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Crafting: a sleeve holding a record -> the record (the emptied sleeve stays in the grid). */
public class SleeveExtractRecipe extends CustomRecipe {
    public SleeveExtractRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack sleeve = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (SleeveOps.isSleeve(stack) && SleeveOps.hasContainedRecord(stack) && sleeve.isEmpty()) {
                sleeve = stack;
            } else {
                return false;
            }
        }
        return !sleeve.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (SleeveOps.isSleeve(stack) && SleeveOps.hasContainedRecord(stack)) {
                return SleeveOps.containedRecord(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (SleeveOps.isSleeve(stack) && SleeveOps.hasContainedRecord(stack)) {
                remaining.set(i, SleeveOps.emptiedSleeve(stack)); // keep the now-empty sleeve
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SLEEVE_EXTRACT.get();
    }
}
