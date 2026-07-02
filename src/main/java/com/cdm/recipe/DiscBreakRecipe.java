package com.cdm.recipe;

import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;
import com.cdm.registry.ModRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * "Scratching" a record at the smithing table: each pass with a flint stylus wears the groove to the
 * next stage — 50, then 75, then a fully broken 100 — letting players deliberately distress a disc.
 * The base disc is consumed and replaced by the more-worn copy.
 */
public class DiscBreakRecipe implements SmithingRecipe {
    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return isBaseIngredient(input.base()) && isTemplateIngredient(input.template());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack out = input.base().copy();
        out.setCount(1);
        int max = out.getMaxDamage();
        float worn = (float) out.getDamageValue() / max;
        // Stages are percentages of the (configurable) wear budget: 50% -> 75% -> fully worn.
        int next = worn < 0.5F ? Math.round(max * 0.5F) : worn < 0.75F ? Math.round(max * 0.75F) : max;
        out.setDamageValue(Math.min(max, next));
        return out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY; // result depends on the input disc
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.is(Items.FLINT);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.is(ModItems.MUSIC_DISC.get())
                && stack.has(ModComponents.RECORD_CONTENT.get())
                && stack.isDamageableItem()
                && stack.getDamageValue() < stack.getMaxDamage();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(Items.FLINT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DISC_BREAK.get();
    }

    public static class Serializer implements RecipeSerializer<DiscBreakRecipe> {
        private static final MapCodec<DiscBreakRecipe> CODEC = MapCodec.unit(DiscBreakRecipe::new);
        // Stateless recipe: encode nothing, decode a fresh instance. (StreamCodec.unit would throw on
        // encode because each loaded recipe is a distinct, non-equal instance.)
        private static final StreamCodec<RegistryFriendlyByteBuf, DiscBreakRecipe> STREAM_CODEC =
                StreamCodec.of((buffer, recipe) -> { }, buffer -> new DiscBreakRecipe());

        @Override
        public MapCodec<DiscBreakRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DiscBreakRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
