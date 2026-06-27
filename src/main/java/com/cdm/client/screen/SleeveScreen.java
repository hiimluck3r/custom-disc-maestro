package com.cdm.client.screen;

import com.cdm.menu.SleeveMenu;
import com.cdm.registry.ModItems;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * One-slot "drop a record in the sleeve" GUI, opened by right-clicking a sleeve. A ghost disc shows
 * where the record goes.
 */
public class SleeveScreen extends AbstractContainerScreen<SleeveMenu> {
    public SleeveScreen(SleeveMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        gg.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E222C);
        gg.fill(x + 3, y + 3, x + imageWidth - 3, y + 72, 0xFF2A3140);
        gg.fill(x + 3, y + 74, x + imageWidth - 3, y + imageHeight - 3, 0xFF2A3140);

        SlotIcons.drawSlot(gg, x + 80, y + 35);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) SlotIcons.drawSlot(gg, x + 8 + c * 18, y + 84 + r * 18);
        for (int c = 0; c < 9; c++) SlotIcons.drawSlot(gg, x + 8 + c * 18, y + 142);

        if (!menu.slots.get(SleeveMenu.DISC_SLOT).hasItem()) {
            SlotIcons.ghost(gg, new ItemStack(ModItems.MUSIC_DISC.get()), x + 80, y + 35);
        }
    }

    private static final java.util.Map<Integer, Component> HINTS =
            java.util.Map.of(SleeveMenu.DISC_SLOT, Component.translatable("cdm.sleeve.insert_hint"));

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
