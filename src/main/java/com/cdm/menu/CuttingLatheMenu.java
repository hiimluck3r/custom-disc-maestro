package com.cdm.menu;

import javax.annotation.Nullable;

import com.cdm.block.entity.CuttingLatheBlockEntity;
import com.cdm.registry.ModBlocks;
import com.cdm.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for the Cutting Lathe. The sequencer fills the top of the screen; the bottom carries the blank
 * input + master output slots and the player inventory, so discs move in and out by hand. Recording is
 * still validated server-side (see {@code LatheRecordPayload}).
 */
public class CuttingLatheMenu extends AbstractContainerMenu {
    private static final int TE_SLOTS = 2;

    // Slot positions (relative to the GUI origin); shared with CuttingLatheScreen's slot drawing.
    public static final int BLANK_X = 104, BLANK_Y = 230;
    public static final int OUTPUT_X = 134, OUTPUT_Y = 230;
    public static final int INV_X = 47, INV_Y = 262, HOTBAR_Y = 320;

    @Nullable
    private final CuttingLatheBlockEntity be;
    private final BlockPos pos;

    public CuttingLatheMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()));
    }

    public CuttingLatheMenu(int id, Inventory inv, @Nullable CuttingLatheBlockEntity be) {
        super(ModMenus.CUTTING_LATHE.get(), id);
        this.be = be;
        this.pos = be != null ? be.getBlockPos() : BlockPos.ZERO;
        IItemHandler handler = be != null ? be.getItems() : new ItemStackHandler(TE_SLOTS);

        this.addSlot(new SlotItemHandler(handler, CuttingLatheBlockEntity.SLOT_BLANK, BLANK_X, BLANK_Y));
        this.addSlot(new SlotItemHandler(handler, CuttingLatheBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Nullable
    private static CuttingLatheBlockEntity resolve(Inventory inv, BlockPos pos) {
        BlockEntity found = inv.player.level().getBlockEntity(pos);
        return found instanceof CuttingLatheBlockEntity p ? p : null;
    }

    public boolean isAt(BlockPos other) {
        return this.pos.equals(other);
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player p) {
        return be != null && be.stillValid(p)
                && p.level().getBlockState(pos).is(ModBlocks.CUTTING_LATHE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int invStart = TE_SLOTS;
        int invEnd = this.slots.size();
        int hotbarStart = invEnd - 9;

        if (index < TE_SLOTS) {
            // Lathe slot -> player inventory.
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, CuttingLatheBlockEntity.SLOT_BLANK,
                CuttingLatheBlockEntity.SLOT_BLANK + 1, false)) {
            // Not a blank (or input full) -> shuffle between inventory and hotbar like vanilla.
            if (index < hotbarStart) {
                if (!this.moveItemStackTo(stack, hotbarStart, invEnd, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, invStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }
}
