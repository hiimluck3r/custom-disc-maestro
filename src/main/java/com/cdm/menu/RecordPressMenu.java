package com.cdm.menu;

import javax.annotation.Nullable;

import com.cdm.block.entity.RecordPressBlockEntity;
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

public class RecordPressMenu extends AbstractContainerMenu {
    private static final int TE_SLOTS = 6;

    @Nullable
    private final RecordPressBlockEntity be;
    private final BlockPos pos;

    public RecordPressMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()));
    }

    public RecordPressMenu(int id, Inventory inv, @Nullable RecordPressBlockEntity be) {
        super(ModMenus.RECORD_PRESS.get(), id);
        this.be = be;
        this.pos = be != null ? be.getBlockPos() : BlockPos.ZERO;
        IItemHandler handler = be != null ? be.getItems() : new ItemStackHandler(TE_SLOTS);

        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_MATRIX, 8, 18));
        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_BLANK, 30, 18));
        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_VINYL_DYE, 8, 52));
        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_TEMPLATE, 46, 52));
        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_PATTERN_DYE, 84, 52));
        this.addSlot(new SlotItemHandler(handler, RecordPressBlockEntity.SLOT_OUTPUT, 150, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 88 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 148));
        }
    }

    @Nullable
    private static RecordPressBlockEntity resolve(Inventory inv, BlockPos pos) {
        BlockEntity found = inv.player.level().getBlockEntity(pos);
        return found instanceof RecordPressBlockEntity p ? p : null;
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
                && p.level().getBlockState(pos).is(ModBlocks.RECORD_PRESS.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < TE_SLOTS) {
            if (!this.moveItemStackTo(stack, TE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 0, RecordPressBlockEntity.SLOT_OUTPUT, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
