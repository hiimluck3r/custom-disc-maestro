package com.cdm.client.screen;

import com.cdm.block.entity.RecordPressBlockEntity;
import com.cdm.data.DiscDesign;
import com.cdm.item.Patterns;
import com.cdm.menu.RecordPressMenu;
import com.cdm.net.PressActionPayload;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Record Press GUI (slot-driven): put a matrix + blank, optionally a vinyl dye, a pattern template and
 * a pattern dye, and press. Colours come from dyes and the pattern from the template — all consumed.
 * A live preview shows the disc that will come out.
 */
public class RecordPressScreen extends AbstractContainerScreen<RecordPressMenu> {
    private static final int PREVIEW_X = 118, PREVIEW_Y = 18;

    // Worded purpose for each empty input slot (keyed by menu slot index), shown on hover.
    private static final java.util.Map<Integer, Component> HINTS = java.util.Map.of(
            RecordPressBlockEntity.SLOT_MATRIX, Component.translatable("cdm.press.hint.matrix"),
            RecordPressBlockEntity.SLOT_BLANK, Component.translatable("cdm.press.hint.blackstone"),
            RecordPressBlockEntity.SLOT_VINYL_DYE, Component.translatable("cdm.press.hint.vinyl_dye"),
            RecordPressBlockEntity.SLOT_TEMPLATE, Component.translatable("cdm.press.hint.template"),
            RecordPressBlockEntity.SLOT_PATTERN_DYE, Component.translatable("cdm.press.hint.pattern_dye"));

    public RecordPressScreen(RecordPressMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 172;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("cdm.press.press"), b -> press())
                .bounds(leftPos + 56, topPos + 18, 56, 16).build());
    }

    private void press() {
        PacketDistributor.sendToServer(new PressActionPayload(menu.getPos()));
    }

    /** The design the current slots would produce (for the preview), mirroring the server logic. */
    private DiscDesign previewDesign() {
        ItemStack vinylDye = menu.slots.get(RecordPressBlockEntity.SLOT_VINYL_DYE).getItem();
        ItemStack template = menu.slots.get(RecordPressBlockEntity.SLOT_TEMPLATE).getItem();
        ItemStack patternDye = menu.slots.get(RecordPressBlockEntity.SLOT_PATTERN_DYE).getItem();
        int vinyl = Patterns.isDye(vinylDye) ? Patterns.dyeColor(vinylDye) : 0x141414;
        int label = Patterns.isDye(patternDye) ? Patterns.dyeColor(patternDye) : 0xFFFFFF;
        return DiscDesign.sanitize(new DiscDesign(vinyl, label, Patterns.indexOf(template)));
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        gg.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E222C);
        gg.fill(x + 3, y + 3, x + imageWidth - 3, y + 78, 0xFF2A3140);
        gg.fill(x + 3, y + 80, x + imageWidth - 3, y + imageHeight - 3, 0xFF2A3140);

        drawSlot(gg, x + 8, y + 18);    // matrix
        drawSlot(gg, x + 30, y + 18);   // blank
        drawSlot(gg, x + 8, y + 52);    // vinyl dye
        drawSlot(gg, x + 46, y + 52);   // template
        drawSlot(gg, x + 84, y + 52);   // pattern dye
        drawSlot(gg, x + 150, y + 18);  // output
        gg.drawString(this.font, ">", x + 140, y + 22, 0xFFB8C0D0, false);

        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) drawSlot(gg, x + 8 + c * 18, y + 88 + r * 18);
        for (int c = 0; c < 9; c++) drawSlot(gg, x + 8 + c * 18, y + 148);

        // Hint icons for what each input slot wants.
        ghostIfEmpty(gg, RecordPressBlockEntity.SLOT_MATRIX, new ItemStack(ModItems.MATRIX.get()), x + 8, y + 18);
        ghostIfEmpty(gg, RecordPressBlockEntity.SLOT_BLANK, new ItemStack(Items.BLACKSTONE), x + 30, y + 18);
        ghostIfEmpty(gg, RecordPressBlockEntity.SLOT_VINYL_DYE, new ItemStack(Items.BLACK_DYE), x + 8, y + 52);
        ghostIfEmpty(gg, RecordPressBlockEntity.SLOT_TEMPLATE, new ItemStack(ModItems.PATTERN_STRIPES.get()), x + 46, y + 52);
        ghostIfEmpty(gg, RecordPressBlockEntity.SLOT_PATTERN_DYE, new ItemStack(Items.WHITE_DYE), x + 84, y + 52);

        // Live preview of the pressed disc.
        ItemStack preview = new ItemStack(ModItems.MUSIC_DISC.get());
        preview.set(ModComponents.DISC_DESIGN.get(), previewDesign());
        gg.renderItem(preview, x + PREVIEW_X, y + PREVIEW_Y);
    }

    private static void drawSlot(GuiGraphics gg, int x, int y) {
        gg.fill(x - 1, y - 1, x + 17, y + 17, 0xFF14161E);
        gg.fill(x, y, x + 16, y + 16, 0xFF3A4250);
    }

    /** Draw a faint hint icon only when the given menu slot is empty. */
    private void ghostIfEmpty(GuiGraphics gg, int slot, ItemStack icon, int x, int y) {
        if (!menu.slots.get(slot).hasItem()) SlotIcons.ghost(gg, icon, x, y);
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
