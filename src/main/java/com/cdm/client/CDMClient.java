package com.cdm.client;

import com.cdm.CDMMod;
import com.cdm.client.screen.CuttingLatheScreen;
import com.cdm.client.screen.PackagingScreen;
import com.cdm.client.screen.RecordPressScreen;
import com.cdm.data.DiscDesign;
import com.cdm.data.SleeveDesign;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModFluids;
import com.cdm.registry.ModItems;
import com.cdm.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only bootstrap: screens, item model predicates, tint handlers and the fluid render. */
@Mod(value = CDMMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CDMMod.MODID, value = Dist.CLIENT)
public class CDMClient {
    public CDMClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static final ResourceLocation DISC_LOOK = ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "disc_look");
    private static final ResourceLocation DISC_BROKEN = ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "disc_broken");
    private static final ResourceLocation SLEEVE_LOOK = ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "sleeve_look");
    private static final ResourceLocation SLEEVE_FILLED = ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "sleeve_filled");

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.MUSIC_DISC.get(), DISC_LOOK,
                    (stack, level, entity, seed) -> {
                        DiscDesign d = stack.get(ModComponents.DISC_DESIGN.get());
                        return d == null ? 0.0F : (d.style() + 1) / (float) (DiscDesign.STYLE_COUNT + 1);
                    });
            // 1.0 once the groove is fully worn (durability exhausted) -> cracked "broken" model.
            ItemProperties.register(ModItems.MUSIC_DISC.get(), DISC_BROKEN,
                    (stack, level, entity, seed) ->
                            stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() ? 1.0F : 0.0F);
            // Sleeve and stencil share the same sleeve_look predicate (sticker index of the design).
            ClampedItemPropertyFunction sleeveLook = (stack, level, entity, seed) -> {
                SleeveDesign d = stack.get(ModComponents.SLEEVE_DESIGN.get());
                return d == null ? 0.0F : d.sticker() / (float) SleeveDesign.STICKER_COUNT;
            };
            ItemProperties.register(ModItems.SLEEVE.get(), SLEEVE_LOOK, sleeveLook);
            ItemProperties.register(ModItems.SLEEVE_STENCIL.get(), SLEEVE_LOOK, sleeveLook);
            // 1.0 while a record is tucked inside -> the disc edge peeks out of the sleeve.
            ClampedItemPropertyFunction sleeveFilled = (stack, level, entity, seed) ->
                    com.cdm.recipe.SleeveOps.hasContainedRecord(stack) ? 1.0F : 0.0F;
            ItemProperties.register(ModItems.SLEEVE.get(), SLEEVE_FILLED, sleeveFilled);
            ItemProperties.register(ModItems.SLEEVE_BLANK.get(), SLEEVE_FILLED, sleeveFilled);
        });
        CDMMod.LOGGER.info("Custom Disc Maestro: client setup complete for {}.",
                Minecraft.getInstance().getUser().getName());
    }

    /** Disc: master untinted; designed disc colours vinyl(0)/label(1). Sleeve: bg(0)/sticker(1). */
    @SubscribeEvent
    static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            DiscDesign d = stack.get(ModComponents.DISC_DESIGN.get());
            if (d == null) return 0xFFFFFFFF;
            return switch (tint) {
                case 0 -> 0xFF000000 | d.vinylColor();
                case 1 -> 0xFF000000 | d.labelColor();
                default -> 0xFFFFFFFF;
            };
        }, ModItems.MUSIC_DISC.get());

        // Filled sleeves carry the untinted disc-peek as layer0, shifting the bg/sticker layers up one.
        event.register((stack, tint) -> {
            SleeveDesign d = stack.get(ModComponents.SLEEVE_DESIGN.get());
            if (d == null) d = SleeveDesign.DEFAULT;
            int shift = com.cdm.recipe.SleeveOps.hasContainedRecord(stack) ? 1 : 0;
            if (tint == shift) return 0xFF000000 | d.bgColor();
            if (tint == shift + 1) return 0xFF000000 | d.stickerColor();
            return 0xFFFFFFFF;
        }, ModItems.SLEEVE.get());

        // Stencil: layer0 = untinted card, layer1 = bg-tinted preview window, layer2 = sticker.
        event.register((stack, tint) -> {
            SleeveDesign d = stack.get(ModComponents.SLEEVE_DESIGN.get());
            if (d == null) d = SleeveDesign.DEFAULT;
            return switch (tint) {
                case 1 -> 0xFF000000 | d.bgColor();
                case 2 -> 0xFF000000 | d.stickerColor();
                default -> 0xFFFFFFFF;
            };
        }, ModItems.SLEEVE_STENCIL.get());
    }

    /** Render the nickel bath using the vanilla water textures, tinted a murky greenish nickel. */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFF12A65C; // rich emerald-green (standard nickel plating bath)
            }
        }, ModFluids.NICKEL_TYPE.get());
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PACKAGING.get(), PackagingScreen::new);
        event.register(ModMenus.CUTTING_LATHE.get(), CuttingLatheScreen::new);
        event.register(ModMenus.RECORD_PRESS.get(), RecordPressScreen::new);
    }
}

