package com.cdm.block.entity;

import com.cdm.CDMConfig;
import com.cdm.audio.NoteSequenceValidator;
import com.cdm.block.CuttingLatheBlock;
import com.cdm.data.DiscMeta;
import com.cdm.data.NoteSequence;
import com.cdm.data.RecordContent;
import com.cdm.menu.CuttingLatheMenu;
import com.cdm.registry.ModBlockEntities;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Backs the Cutting Lathe GUI. The melody itself lives in the open screen and is sent (and re-validated)
 * on record; the lathe holds two items: a blank disc in the input slot and the cut master in the output
 * slot. {@link #cut} consumes the blank and stamps a fresh master from a validated melody + metadata.
 * Slots: 0 blank input, 1 master output.
 */
public class CuttingLatheBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_BLANK = 0;
    public static final int SLOT_OUTPUT = 1;

    /** Outcome of a cut request, so the network handler can give the player a precise reason. */
    public enum CutResult { OK, NO_BLANK, OUTPUT_BLOCKED, EMPTY_MELODY }

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncDiscOnPlatter();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_BLANK && stack.is(ModItems.BLANK_DISC.get());
        }
    };

    /** Mirrors "a disc sits in the lathe" into the HAS_DISC blockstate so it shows on the platter. */
    private void syncDiscOnPlatter() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CuttingLatheBlock.HAS_DISC)) return;
        boolean hasDisc = !items.getStackInSlot(SLOT_BLANK).isEmpty() || !items.getStackInSlot(SLOT_OUTPUT).isEmpty();
        if (state.getValue(CuttingLatheBlock.HAS_DISC) != hasDisc) {
            level.setBlockAndUpdate(worldPosition, state.setValue(CuttingLatheBlock.HAS_DISC, hasDisc));
        }
    }

    public CuttingLatheBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUTTING_LATHE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Build a music disc from an (untrusted) melody + metadata, or {@link ItemStack#EMPTY} if empty. */
    public static ItemStack buildDisc(NoteSequence sequence, DiscMeta meta) {
        NoteSequence safe = NoteSequenceValidator.sanitize(sequence);
        if (safe.notes().isEmpty()) return ItemStack.EMPTY;
        DiscMeta safeMeta = DiscMeta.sanitize(meta);

        ItemStack disc = new ItemStack(ModItems.MUSIC_DISC.get(), 1);
        disc.set(ModComponents.RECORD_CONTENT.get(),
                new RecordContent(RecordContent.Side.ofNotes(safe, safeMeta.title()), RecordContent.Side.EMPTY, 33));
        disc.set(ModComponents.DISC_META.get(), safeMeta);
        disc.set(ModComponents.MASTER.get(), true); // a lathe cut is a master; only masters electroform
        disc.set(DataComponents.MAX_DAMAGE, CDMConfig.MASTER_USES.get()); // admin-configured wear budget
        return disc;
    }

    /** Cut the (untrusted) melody onto a master disc, consuming the blank in the input slot. */
    public CutResult cut(NoteSequence sequence, DiscMeta meta) {
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return CutResult.OUTPUT_BLOCKED;
        }
        if (!items.getStackInSlot(SLOT_BLANK).is(ModItems.BLANK_DISC.get())) {
            return CutResult.NO_BLANK;
        }
        ItemStack disc = buildDisc(sequence, meta);
        if (disc.isEmpty()) {
            return CutResult.EMPTY_MELODY;
        }
        items.setStackInSlot(SLOT_OUTPUT, disc);
        ItemStack blank = items.getStackInSlot(SLOT_BLANK);
        blank.shrink(1);
        items.setStackInSlot(SLOT_BLANK, blank);
        return CutResult.OK;
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cdm.cutting_lathe");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CuttingLatheMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            CompoundTag itemsTag = tag.getCompound("Items").copy();
            // Drop any persisted slot count so older (smaller) saves load into the current handler size.
            itemsTag.remove("Size");
            items.deserializeNBT(registries, itemsTag);
        }
    }
}
