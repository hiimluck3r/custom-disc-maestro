package com.cdm.registry;

import com.cdm.CDMMod;
import com.cdm.item.MatrixItem;
import com.cdm.item.MusicDiscItem;
import com.cdm.item.SleeveItem;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Lean item set: a blank disc to record onto, the finished music disc (vanilla-playable), a galvanic
 * matrix for stamping copies, and the packaging items. Crafting ingredients lean on vanilla
 * (iron, paper, dye, ...) rather than bespoke materials.
 */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CDMMod.MODID);

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.registerSimpleItem(name, new Item.Properties());
    }

    public static final DeferredItem<?> CUTTING_LATHE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CUTTING_LATHE);
    public static final DeferredItem<?> PACKAGING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PACKAGING_TABLE);
    public static final DeferredItem<?> RECORD_PRESS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RECORD_PRESS);

    public static final DeferredItem<Item> BLANK_DISC = simple("blank_disc");

    // Durability models groove WEAR: every jukebox play costs 1 point; a fully worn disc is "broken"
    // (distorted audio + cracked texture) but never destroyed. This is only the REGISTRATION fallback
    // (e.g. creative-menu stacks) — the lathe/press stamp the admin-configured maximum from CDMConfig
    // onto every disc they produce via the max_damage component.
    public static final int MAX_WEAR = 32;
    public static final DeferredItem<MusicDiscItem> MUSIC_DISC = ITEMS.register("music_disc",
            () -> new MusicDiscItem(new Item.Properties().rarity(Rarity.RARE)
                    .durability(MAX_WEAR)
                    .jukeboxPlayable(ModJukeboxSongs.CUSTOM)));

    // The metal negative grown off a master in the kupfernickel bath. Registration fallback only —
    // electroforming stamps the admin-configured press count from CDMConfig onto each matrix.
    public static final int MATRIX_USES = 3;
    public static final DeferredItem<MatrixItem> MATRIX = ITEMS.register("matrix",
            () -> new MatrixItem(new Item.Properties().durability(MATRIX_USES)));

    // Packaging. Both blank and designed sleeves are SleeveItems so either can be packed by RMB.
    public static final DeferredItem<SleeveItem> SLEEVE_BLANK = ITEMS.register("sleeve_blank",
            () -> new SleeveItem(new Item.Properties()));
    public static final DeferredItem<SleeveItem> SLEEVE = ITEMS.register("sleeve",
            () -> new SleeveItem(new Item.Properties()));
    public static final DeferredItem<com.cdm.item.StencilItem> SLEEVE_STENCIL = ITEMS.register("sleeve_stencil",
            () -> new com.cdm.item.StencilItem(new Item.Properties()));

    // Pattern templates: drop when a skeleton kills a skeleton; consumed by the Press / Packaging Table.
    public static final DeferredItem<Item> PATTERN_STRIPES = simple("pattern_stripes");
    public static final DeferredItem<Item> PATTERN_RIBBON = simple("pattern_ribbon");
    public static final DeferredItem<Item> PATTERN_DOTS = simple("pattern_dots");

    public static final DeferredItem<BucketItem> NICKEL_BUCKET = ITEMS.register("kupfernickel_bucket",
            () -> new BucketItem(ModFluids.NICKEL.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET).stacksTo(1)));
}
