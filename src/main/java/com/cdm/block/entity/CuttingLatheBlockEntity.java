package com.cdm.block.entity;

import com.cdm.audio.NoteSequenceValidator;
import com.cdm.data.DiscMeta;
import com.cdm.data.NoteSequence;
import com.cdm.data.RecordContent;
import com.cdm.menu.CuttingLatheMenu;
import com.cdm.registry.ModBlockEntities;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Backs the Cutting Lathe GUI. The lathe holds no items — composition lives entirely in the open
 * screen and is sent (and re-validated) on record. {@link #buildDisc} turns a validated melody +
 * metadata into a fresh music disc; the blank is consumed from, and the disc returned to, the
 * player's inventory by {@code LatheRecordPayload}.
 */
public class CuttingLatheBlockEntity extends BlockEntity implements MenuProvider {
    public CuttingLatheBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUTTING_LATHE.get(), pos, state);
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
        return disc;
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
}
