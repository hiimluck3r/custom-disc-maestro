package com.cdm.menu;

import javax.annotation.Nullable;

import com.cdm.block.entity.PackagingTableBlockEntity;
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

public class PackagingMenu extends AbstractContainerMenu {
    private static final int TE_SLOTS = 5;

    @Nullable
    private final PackagingTableBlockEntity be;
    private final BlockPos pos;

    // Typed slot handles so the screen doesn't depend on raw slot ordering.
    private final Slot sleeveSlot;
    private final Slot bgDyeSlot;
    private final Slot templateSlot;
    private final Slot patternDyeSlot;

    /** Client-side constructor (from the open-menu packet). */
    public PackagingMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()));
    }

    /** Server-side constructor. */
    public PackagingMenu(int id, Inventory inv, @Nullable PackagingTableBlockEntity be) {
        super(ModMenus.PACKAGING.get(), id);
        this.be = be;
        this.pos = be != null ? be.getBlockPos() : BlockPos.ZERO;
        IItemHandler handler = be != null ? be.getItems() : new ItemStackHandler(TE_SLOTS);

        // No record slot — records are sleeved with bundle-style clicks on the sleeve item itself.
        this.sleeveSlot = this.addSlot(new SlotItemHandler(handler, PackagingTableBlockEntity.SLOT_SLEEVE, 8, 20));
        this.addSlot(new SlotItemHandler(handler, PackagingTableBlockEntity.SLOT_OUTPUT, 150, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // output only
            }
        });
        this.bgDyeSlot = this.addSlot(new SlotItemHandler(handler, PackagingTableBlockEntity.SLOT_BG_DYE, 8, 76));
        this.templateSlot = this.addSlot(new SlotItemHandler(handler, PackagingTableBlockEntity.SLOT_TEMPLATE, 46, 76));
        this.patternDyeSlot = this.addSlot(new SlotItemHandler(handler, PackagingTableBlockEntity.SLOT_PATTERN_DYE, 84, 76));

        // Player inventory (3x9) + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 166));
        }
    }

    @Nullable
    private static PackagingTableBlockEntity resolve(Inventory inv, BlockPos pos) {
        BlockEntity found = inv.player.level().getBlockEntity(pos);
        return found instanceof PackagingTableBlockEntity p ? p : null;
    }

    public boolean isAt(BlockPos other) {
        return this.pos.equals(other);
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack sleeveItem() {
        return sleeveSlot.getItem();
    }

    public ItemStack bgDyeItem() {
        return bgDyeSlot.getItem();
    }

    public ItemStack templateItem() {
        return templateSlot.getItem();
    }

    public ItemStack patternDyeItem() {
        return patternDyeSlot.getItem();
    }

    @Override
    public boolean stillValid(Player p) {
        return be != null && be.stillValid(p)
                && p.level().getBlockState(pos).is(ModBlocks.PACKAGING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return moved;
        ItemStack stack = slot.getItem();
        moved = stack.copy();
        int invStart = TE_SLOTS;
        int invEnd = this.slots.size();
        if (index < TE_SLOTS) {
            // From the table into the player inventory.
            if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else {
            // From the player inventory into any accepting table slot (output rejects via mayPlace).
            if (!this.moveItemStackTo(stack, 0, TE_SLOTS, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
