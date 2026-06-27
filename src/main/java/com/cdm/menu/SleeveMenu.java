package com.cdm.menu;

import com.cdm.item.SleeveItem;
import com.cdm.recipe.SleeveOps;
import com.cdm.registry.ModMenus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A tiny one-slot menu opened by right-clicking a sleeve in hand: drop any music disc in to package it.
 * The disc lives in the sleeve's CONTAINED_RECORD component; this menu just edits that, writing back to
 * the held sleeve when closed. No block/table needed.
 */
public class SleeveMenu extends AbstractContainerMenu {
    public static final int DISC_SLOT = 0;

    private final Player player;
    private final InteractionHand hand;
    private final SimpleContainer disc = new SimpleContainer(1);

    /** Client-side constructor (from the open-menu packet). */
    public SleeveMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readEnum(InteractionHand.class));
    }

    /** Shared constructor: {@code hand} identifies which held sleeve we are editing. */
    public SleeveMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenus.SLEEVE.get(), id);
        this.player = inv.player;
        this.hand = hand;

        ItemStack sleeve = player.getItemInHand(hand);
        ItemStack contained = SleeveOps.containedRecord(sleeve);
        if (!contained.isEmpty()) {
            disc.setItem(DISC_SLOT, contained);
        }

        this.addSlot(new Slot(disc, DISC_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isDisc(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private static boolean isDisc(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    private boolean holdingSleeve() {
        return player.getItemInHand(hand).getItem() instanceof SleeveItem;
    }

    @Override
    public boolean stillValid(Player p) {
        return holdingSleeve();
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p.level().isClientSide) return;
        ItemStack sleeve = p.getItemInHand(hand);
        ItemStack inside = disc.getItem(DISC_SLOT);
        if (sleeve.getItem() instanceof SleeveItem) {
            SleeveOps.setContainedRecord(sleeve, inside);
        } else if (!inside.isEmpty()) {
            // Sleeve no longer in hand — return the disc to the inventory rather than losing it.
            p.getInventory().placeItemBackInInventory(inside);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index == DISC_SLOT) {
            // Disc slot -> player inventory.
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (isDisc(stack)) {
            // Inventory -> disc slot.
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
