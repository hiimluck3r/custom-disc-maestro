package com.cdm.client.screen;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared slot drawing for CDM's dark-theme GUIs, plus a dimmed "ghost" hint icon for empty slots. */
final class SlotIcons {
    private SlotIcons() {}

    /** A 16px slot well matching the CDM panel theme. */
    static void drawSlot(GuiGraphics gg, int x, int y) {
        gg.fill(x - 1, y - 1, x + 17, y + 17, 0xFF14161E);
        gg.fill(x, y, x + 16, y + 16, 0xFF3A4250);
    }

    /** Draw a faint hint item in an empty slot so players see what belongs there. */
    static void ghost(GuiGraphics gg, ItemStack icon, int x, int y) {
        if (icon.isEmpty()) return;
        gg.renderFakeItem(icon, x, y);
        // Dim it (drawn above the item via a raised Z) so it reads as a hint, not a real item.
        gg.pose().pushPose();
        gg.pose().translate(0, 0, 300);
        gg.fill(x, y, x + 16, y + 16, 0xAA141821);
        gg.pose().popPose();
    }

    /**
     * When an empty input slot is hovered, show its purpose as a worded tooltip (the slot's framed
     * ghost icon is the always-on hint; the words appear only on demand). {@code hints} is keyed by
     * the slot's menu index.
     */
    static void emptyHint(GuiGraphics gg, Slot hovered, int mouseX, int mouseY, Map<Integer, Component> hints) {
        if (hovered == null || hovered.hasItem()) return;
        Component text = hints.get(hovered.index);
        if (text != null) {
            gg.renderTooltip(Minecraft.getInstance().font, text, mouseX, mouseY);
        }
    }
}
