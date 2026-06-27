package com.cdm.item;

import java.util.List;
import java.util.function.Supplier;

import com.cdm.data.DiscMeta;
import com.cdm.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shared display of a recorded item's {@link DiscMeta} (title / author / album). Used by both the
 * music disc and the galvanic matrix so a matrix shows which song it carries, just like the disc.
 */
public final class DiscNaming {
    private DiscNaming() {}

    public static Component name(ItemStack stack, Supplier<Component> fallback) {
        DiscMeta meta = stack.get(ModComponents.DISC_META.get());
        if (meta == null || meta.isEmpty()) {
            return fallback.get();
        }
        String title = meta.title().isBlank() ? fallback.get().getString() : meta.title();
        return meta.author().isBlank()
                ? Component.literal(title)
                : Component.literal(title + " — " + meta.author());
    }

    public static void appendMeta(ItemStack stack, List<Component> tooltip) {
        DiscMeta meta = stack.get(ModComponents.DISC_META.get());
        if (meta == null || meta.isEmpty()) {
            return;
        }
        if (!meta.author().isBlank()) {
            tooltip.add(Component.translatable("item.cdm.music_disc.by", meta.author()).withStyle(ChatFormatting.GRAY));
        }
        meta.album().ifPresent(album ->
                tooltip.add(Component.translatable("item.cdm.music_disc.album", album).withStyle(ChatFormatting.DARK_GRAY)));
    }
}
