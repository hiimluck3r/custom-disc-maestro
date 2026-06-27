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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the Cutting Lathe. The lathe is a pure composition editor: it has NO slots and does not
 * show the player inventory. Recording pulls a blank disc straight from the player's inventory and
 * hands back the finished disc (see {@code LatheRecordPayload}), which leaves the whole screen free
 * for the sequencer.
 */
public class CuttingLatheMenu extends AbstractContainerMenu {
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
        return ItemStack.EMPTY; // no slots
    }
}
