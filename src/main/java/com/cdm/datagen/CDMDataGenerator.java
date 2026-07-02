package com.cdm.datagen;

import com.cdm.CDMMod;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Data generation entry point. M1: generates placeholder block/item models so the registry skeleton
 * renders cleanly (all pointing at a single placeholder texture). Real art and recipe/tag/loot
 * providers are added in later milestones.
 */
@EventBusSubscriber(modid = CDMMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CDMDataGenerator {
    private CDMDataGenerator() {}

    static final String[] BLOCKS = {
            "cutting_lathe", "packaging_table", "record_press"
    };

    static final String[] SIMPLE_ITEMS = {
            "blank_disc", "matrix",
            "pattern_stripes", "pattern_ribbon", "pattern_dots"
    };

    static final String[] DISC_STYLES = {"plain", "stripes", "ribbon", "dots"};
    static final String[] SLEEVE_STICKERS = {"stripes", "ribbon", "dots"}; // index 1..3; 0 = none

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new Blocks(output, helper));
        gen.addProvider(event.includeClient(), new Items(output, helper));
    }

    private static class Blocks extends BlockStateProvider {
        Blocks(PackOutput output, ExistingFileHelper helper) {
            super(output, CDMMod.MODID, helper);
        }

        @Override
        protected void registerStatesAndModels() {
            // Furnace-style worktables: one front face turned toward the placer, three matching
            // sides. The lathe/press top swaps to a with-disc texture while a record is inside.
            discTable("cutting_lathe", modLoc("block/cutting_lathe_side"));
            discTable("record_press", modLoc("block/record_press_side"));
            horizontalBlock(blockNamed("packaging_table"),
                    tableModel("packaging_table", "", mcLoc("block/spruce_planks")));
        }

        private ModelFile tableModel(String name, String suffix, ResourceLocation bottom) {
            return models().orientableWithBottom(name + suffix,
                    modLoc("block/" + name + "_side"),
                    modLoc("block/" + name + "_front"),
                    bottom,
                    modLoc("block/" + name + "_top" + suffix));
        }

        private void discTable(String name, ResourceLocation bottom) {
            ModelFile empty = tableModel(name, "", bottom);
            ModelFile withDisc = tableModel(name, "_disc", bottom);
            getVariantBuilder(blockNamed(name)).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(state.getValue(BlockStateProperties.HAS_RECORD) ? withDisc : empty)
                    .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360)
                    .build());
        }

        private net.minecraft.world.level.block.Block blockNamed(String name) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .get(ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, name));
        }
    }

    private static class Items extends ItemModelProvider {
        Items(PackOutput output, ExistingFileHelper helper) {
            super(output, CDMMod.MODID, helper);
        }

        @Override
        protected void registerModels() {
            ModelFile generated = new ModelFile.UncheckedModelFile("item/generated");
            // Block items inherit their block model.
            for (String name : BLOCKS) {
                withExistingParent(name, modLoc("block/" + name));
            }
            // Simple items use a flat generated model over their own texture.
            for (String name : SIMPLE_ITEMS) {
                getBuilder(name).parent(generated).texture("layer0", modLoc("item/" + name));
            }

            // Music disc: base = white master; a DiscDesign switches (via cdm:disc_look) to a layered,
            // tinted model per label style (layer0 vinyl, layer1 label, layer2 frame).
            for (int s = 0; s < DISC_STYLES.length; s++) {
                getBuilder("music_disc_style_" + s).parent(generated)
                        .texture("layer0", modLoc("item/disc_vinyl"))
                        .texture("layer1", modLoc("item/disc_label_" + DISC_STYLES[s]))
                        .texture("layer2", modLoc("item/disc_frame"));
            }
            // Broken variants reuse the tinted design layers with a "disc 11"-style bite + drifting
            // shard, so a shattered record still shows its own colours and label pattern.
            for (int s = 0; s < DISC_STYLES.length; s++) {
                getBuilder("music_disc_broken_style_" + s).parent(generated)
                        .texture("layer0", modLoc("item/disc_vinyl_broken"))
                        .texture("layer1", modLoc("item/disc_label_" + DISC_STYLES[s] + "_broken"))
                        .texture("layer2", modLoc("item/disc_frame_broken"));
            }
            getBuilder("music_disc_broken").parent(generated).texture("layer0", modLoc("item/disc_master_broken"));
            var disc = getBuilder("music_disc").parent(generated).texture("layer0", modLoc("item/disc_master"));
            for (int s = 0; s < DISC_STYLES.length; s++) {
                float threshold = (s + 1) / (float) (DISC_STYLES.length + 1);
                disc.override()
                        .predicate(modLoc("disc_look"), threshold)
                        .model(new ModelFile.UncheckedModelFile(modLoc("item/music_disc_style_" + s)))
                        .end();
            }
            // A fully worn MASTER (no design) shows the shattered master model...
            disc.override()
                    .predicate(modLoc("disc_broken"), 0.5F)
                    .model(new ModelFile.UncheckedModelFile(modLoc("item/music_disc_broken")))
                    .end();
            // ...while designed discs match a per-style broken override (added last -> wins), keeping
            // their vinyl/label colours on the shattered model.
            for (int s = 0; s < DISC_STYLES.length; s++) {
                float threshold = (s + 1) / (float) (DISC_STYLES.length + 1);
                disc.override()
                        .predicate(modLoc("disc_look"), threshold)
                        .predicate(modLoc("disc_broken"), 0.5F)
                        .model(new ModelFile.UncheckedModelFile(modLoc("item/music_disc_broken_style_" + s)))
                        .end();
            }

            // Sleeve: base = bg-only envelope (sticker 0); a sticker preset adds a tinted overlay.
            // Filled variants put the untinted disc-peek UNDER the envelope (layer0), so a contained
            // record's edge shows above the top edge; the colour handler shifts tints accordingly.
            int stickerCount = SLEEVE_STICKERS.length + 1; // include "none"
            for (int s = 0; s < SLEEVE_STICKERS.length; s++) {
                getBuilder("sleeve_sticker_" + (s + 1)).parent(generated)
                        .texture("layer0", modLoc("item/sleeve_base"))
                        .texture("layer1", modLoc("item/sleeve_sticker_" + SLEEVE_STICKERS[s]));
                getBuilder("sleeve_sticker_" + (s + 1) + "_filled").parent(generated)
                        .texture("layer0", modLoc("item/disc_peek"))
                        .texture("layer1", modLoc("item/sleeve_base"))
                        .texture("layer2", modLoc("item/sleeve_sticker_" + SLEEVE_STICKERS[s]));
            }
            getBuilder("sleeve_filled").parent(generated)
                    .texture("layer0", modLoc("item/disc_peek"))
                    .texture("layer1", modLoc("item/sleeve_base"));
            var sleeve = getBuilder("sleeve").parent(generated).texture("layer0", modLoc("item/sleeve_base"));
            for (int s = 1; s <= SLEEVE_STICKERS.length; s++) {
                sleeve.override()
                        .predicate(modLoc("sleeve_look"), s / (float) stickerCount)
                        .model(new ModelFile.UncheckedModelFile(modLoc("item/sleeve_sticker_" + s)))
                        .end();
            }
            sleeve.override()
                    .predicate(modLoc("sleeve_filled"), 0.5F)
                    .model(new ModelFile.UncheckedModelFile(modLoc("item/sleeve_filled")))
                    .end();
            for (int s = 1; s <= SLEEVE_STICKERS.length; s++) {
                sleeve.override()
                        .predicate(modLoc("sleeve_look"), s / (float) stickerCount)
                        .predicate(modLoc("sleeve_filled"), 0.5F)
                        .model(new ModelFile.UncheckedModelFile(modLoc("item/sleeve_sticker_" + s + "_filled")))
                        .end();
            }

            // Blank sleeve: plain paper envelope, same peek treatment (it can hold a record too).
            getBuilder("sleeve_blank_filled").parent(generated)
                    .texture("layer0", modLoc("item/disc_peek"))
                    .texture("layer1", modLoc("item/sleeve_blank"));
            getBuilder("sleeve_blank").parent(generated)
                    .texture("layer0", modLoc("item/sleeve_blank"))
                    .override()
                    .predicate(modLoc("sleeve_filled"), 0.5F)
                    .model(new ModelFile.UncheckedModelFile(modLoc("item/sleeve_blank_filled")))
                    .end();

            // Stencil: a stencil-card base (layer0, untinted) plus the design's tinted bg (layer1) and
            // a mini-motif fitted to the card window (layer2), so each template is distinguishable.
            for (int s = 0; s < SLEEVE_STICKERS.length; s++) {
                getBuilder("sleeve_stencil_sticker_" + (s + 1)).parent(generated)
                        .texture("layer0", modLoc("item/sleeve_stencil"))
                        .texture("layer1", modLoc("item/stencil_window"))
                        .texture("layer2", modLoc("item/stencil_sticker_" + (s + 1)));
            }
            var stencil = getBuilder("sleeve_stencil").parent(generated)
                    .texture("layer0", modLoc("item/sleeve_stencil"))
                    .texture("layer1", modLoc("item/stencil_window"));
            for (int s = 1; s <= SLEEVE_STICKERS.length; s++) {
                stencil.override()
                        .predicate(modLoc("sleeve_look"), s / (float) stickerCount)
                        .model(new ModelFile.UncheckedModelFile(modLoc("item/sleeve_stencil_sticker_" + s)))
                        .end();
            }
        }
    }
}
