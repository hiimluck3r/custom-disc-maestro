package com.cdm.recipe;

import com.cdm.registry.ModComponents;
import com.cdm.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Crafting: a sleeve (without a record) + a vinyl record -> the sleeve holding that record. */
public class SleeveInsertRecipe extends CustomRecipe {
    public SleeveInsertRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack sleeve = ItemStack.EMPTY;
        ItemStack record = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (SleeveOps.isSleeve(stack) && !SleeveOps.hasContainedRecord(stack) && sleeve.isEmpty()) {
                sleeve = stack;
            } else if (SleeveOps.isRecord(stack) && record.isEmpty()) {
                record = stack;
            } else {
                return false; // anything unexpected (or a second sleeve/record) invalidates it
            }
        }
        return !sleeve.isEmpty() && !record.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack sleeve = ItemStack.EMPTY;
        ItemStack record = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (SleeveOps.isSleeve(stack) && !SleeveOps.hasContainedRecord(stack)) sleeve = stack;
            else if (SleeveOps.isRecord(stack)) record = stack;
        }
        if (sleeve.isEmpty() || record.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = sleeve.copyWithCount(1);
        SleeveOps.setContainedRecord(result, record);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SLEEVE_INSERT.get();
    }
}
