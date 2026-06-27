package com.cdm.registry;

import java.util.function.Supplier;

import com.cdm.CDMMod;
import com.cdm.recipe.DiscBreakRecipe;
import com.cdm.recipe.SleeveExtractRecipe;
import com.cdm.recipe.SleeveInsertRecipe;
import com.cdm.recipe.SleeveStencilRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CDMMod.MODID);

    public static final Supplier<RecipeSerializer<SleeveInsertRecipe>> SLEEVE_INSERT =
            SERIALIZERS.register("sleeve_insert", () -> new SimpleCraftingRecipeSerializer<>(SleeveInsertRecipe::new));

    public static final Supplier<RecipeSerializer<SleeveExtractRecipe>> SLEEVE_EXTRACT =
            SERIALIZERS.register("sleeve_extract", () -> new SimpleCraftingRecipeSerializer<>(SleeveExtractRecipe::new));

    public static final Supplier<RecipeSerializer<SleeveStencilRecipe>> SLEEVE_STENCIL_STAMP =
            SERIALIZERS.register("sleeve_stencil_stamp", () -> new SimpleCraftingRecipeSerializer<>(SleeveStencilRecipe::new));

    // Smithing-table "scratch": wears a disc to the next broken stage (50 -> 75 -> 100).
    public static final Supplier<RecipeSerializer<DiscBreakRecipe>> DISC_BREAK =
            SERIALIZERS.register("disc_break", DiscBreakRecipe.Serializer::new);
}
