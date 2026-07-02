package com.cdm.client.screen;

import com.cdm.data.SleeveDesign;
import com.cdm.item.Patterns;
import com.cdm.menu.PackagingMenu;
import com.cdm.net.PackagingActionPayload;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Packaging Table GUI (slot-driven): put a background dye, a pattern template and a pattern dye to style
 * a sleeve (with a live preview), apply it, or bake a reusable stencil. Colours come from the dyes and
 * the pattern from the template — consumed on Apply. Records go in by right-clicking the sleeve.
 */
public class PackagingScreen extends AbstractContainerScreen<PackagingMenu> {
    private static final int PREVIEW_X = 118, PREVIEW_Y = 19, ACT_Y = 40, TITLE_Y = 58;

    // Worded purpose for each empty input slot, keyed by MENU slot index (no record slot here):
    // 0 sleeve, 1 output, 2 bg dye, 3 template, 4 pattern dye.
    private static final java.util.Map<Integer, Component> HINTS = java.util.Map.of(
            0, Component.translatable("cdm.packaging.hint.sleeve"),
            2, Component.translatable("cdm.packaging.hint.bg"),
            3, Component.translatable("cdm.packaging.hint.template"),
            4, Component.translatable("cdm.packaging.hint.pattern"));

    private EditBox titleBox;

    // Ghost hint icons + the live-preview sleeve, built once and reused every frame.
    private final ItemStack sleeveGhost = new ItemStack(ModItems.SLEEVE_BLANK.get());
    private final ItemStack bgDyeGhost = new ItemStack(Items.WHITE_DYE);
    private final ItemStack templateGhost = new ItemStack(ModItems.PATTERN_STRIPES.get());
    private final ItemStack patternDyeGhost = new ItemStack(Items.BLACK_DYE);
    private final ItemStack preview = new ItemStack(ModItems.SLEEVE.get());

    public PackagingScreen(PackagingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 190;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleBox = new EditBox(this.font, leftPos + 8, topPos + TITLE_Y, 160, 12,
                Component.translatable("cdm.packaging.title_field"));
        titleBox.setMaxLength(SleeveDesign.MAX_TITLE);
        titleBox.setHint(Component.translatable("cdm.packaging.title_field"));
        ItemStack sleeve = this.menu.sleeveItem();
        SleeveDesign d = sleeve.get(ModComponents.SLEEVE_DESIGN.get());
        if (d != null && !d.title().isBlank()) titleBox.setValue(d.title());
        addRenderableWidget(titleBox);

        // Insert/extract moved to right-clicking the sleeve; the table only designs + bakes stencils.
        addAction("cdm.packaging.apply", PackagingActionPayload.APPLY_DESIGN, 8, 78);
        addAction("cdm.packaging.stencil", PackagingActionPayload.MAKE_STENCIL, 98, 78);
    }

    private void addAction(String key, int action, int x, int w) {
        addRenderableWidget(Button.builder(Component.translatable(key), b -> send(action))
                .bounds(leftPos + x, topPos + ACT_Y, w, 14).build());
    }

    private void send(int action) {
        String title = action == PackagingActionPayload.APPLY_DESIGN && titleBox != null ? titleBox.getValue() : "";
        PacketDistributor.sendToServer(new PackagingActionPayload(menu.getPos(), action, title));
    }

    private SleeveDesign previewDesign() {
        ItemStack bgDye = menu.bgDyeItem();
        ItemStack template = menu.templateItem();
        ItemStack patternDye = menu.patternDyeItem();
        int bg = Patterns.isDye(bgDye) ? Patterns.dyeColor(bgDye) : 0xE8E8E8;
        int sticker = Patterns.isDye(patternDye) ? Patterns.dyeColor(patternDye) : 0x303030;
        return new SleeveDesign(bg, sticker, Patterns.indexOf(template),
                titleBox != null ? titleBox.getValue() : "");
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (titleBox != null && titleBox.canConsumeInput()) {
            titleBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        gg.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E222C);
        gg.fill(x + 3, y + 3, x + imageWidth - 3, y + 96, 0xFF2A3140);
        gg.fill(x + 3, y + 98, x + imageWidth - 3, y + imageHeight - 3, 0xFF2A3140);

        SlotIcons.drawSlot(gg, x + 8, y + 20);    // sleeve
        SlotIcons.drawSlot(gg, x + 150, y + 20);  // output (stencil)
        gg.drawString(this.font, ">", x + 140, y + 24, 0xFFB8C0D0, false);
        SlotIcons.drawSlot(gg, x + 8, y + 76);    // bg dye
        SlotIcons.drawSlot(gg, x + 46, y + 76);   // template
        SlotIcons.drawSlot(gg, x + 84, y + 76);   // pattern dye

        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SlotIcons.drawSlot(gg, x + 8 + c * 18, y + 106 + r * 18);
        for (int c = 0; c < 9; c++) SlotIcons.drawSlot(gg, x + 8 + c * 18, y + 166);

        // Hint icons so it's clear what each slot wants.
        if (menu.sleeveItem().isEmpty()) SlotIcons.ghost(gg, sleeveGhost, x + 8, y + 20);
        if (menu.bgDyeItem().isEmpty()) SlotIcons.ghost(gg, bgDyeGhost, x + 8, y + 76);
        if (menu.templateItem().isEmpty()) SlotIcons.ghost(gg, templateGhost, x + 46, y + 76);
        if (menu.patternDyeItem().isEmpty()) SlotIcons.ghost(gg, patternDyeGhost, x + 84, y + 76);

        // Live preview of the designed sleeve.
        preview.set(ModComponents.SLEEVE_DESIGN.get(), previewDesign());
        gg.renderItem(preview, x + PREVIEW_X, y + PREVIEW_Y);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        this.renderTooltip(gg, mouseX, mouseY);
        SlotIcons.emptyHint(gg, this.hoveredSlot, mouseX, mouseY, HINTS);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, this.titleLabelX, 6, 0xFFE8E8E8, false);
        gg.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8C0D0, false);
    }
}
