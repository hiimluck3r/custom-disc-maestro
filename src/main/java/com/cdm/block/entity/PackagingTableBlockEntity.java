package com.cdm.block.entity;

import com.cdm.data.SleeveDesign;
import com.cdm.item.Patterns;
import com.cdm.menu.PackagingMenu;
import com.cdm.recipe.SleeveOps;
import com.cdm.registry.ModBlockEntities;
import com.cdm.registry.ModComponents;
import com.cdm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
 * Backs the Packaging Table GUI. Slots: 0 = sleeve being worked on, 1 = record to insert/extract,
 * 2 = stencil output. All mutating actions are driven by validated server-side requests
 * (PackagingActionPayload), never directly by the client.
 */
public class PackagingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_SLEEVE = 0;
    public static final int SLOT_RECORD = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_BG_DYE = 3;
    public static final int SLOT_TEMPLATE = 4;
    public static final int SLOT_PATTERN_DYE = 5;

    private static final int DEFAULT_BG = 0xE8E8E8;
    private static final int DEFAULT_STICKER = 0x303030;

    private final ItemStackHandler items = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_SLEEVE -> isSleeve(stack);
                case SLOT_RECORD -> SleeveOps.isRecord(stack);
                case SLOT_BG_DYE, SLOT_PATTERN_DYE -> Patterns.isDye(stack);
                case SLOT_TEMPLATE -> Patterns.isTemplate(stack);
                default -> false; // output slot is filled by the BE only
            };
        }
    };

    public PackagingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PACKAGING_TABLE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    // --- server-side actions (called from validated payloads) ---

    /** Apply a design derived from the dye + template slots (all consumed); {@code title} is free text. */
    public void applyDesign(String title) {
        ItemStack sleeve = items.getStackInSlot(SLOT_SLEEVE);
        if (!isSleeve(sleeve)) return;
        if (hasContainedRecord(sleeve)) return; // sealed packaging — design is locked once a record is in

        ItemStack bgDye = items.getStackInSlot(SLOT_BG_DYE);
        ItemStack template = items.getStackInSlot(SLOT_TEMPLATE);
        ItemStack patternDye = items.getStackInSlot(SLOT_PATTERN_DYE);
        int bg = Patterns.isDye(bgDye) ? Patterns.dyeColor(bgDye) : DEFAULT_BG;
        int stickerColor = Patterns.isDye(patternDye) ? Patterns.dyeColor(patternDye) : DEFAULT_STICKER;
        int sticker = Patterns.indexOf(template);
        SleeveDesign design = SleeveDesign.sanitize(new SleeveDesign(bg, stickerColor, sticker, title));

        ItemStack designed;
        if (sleeve.is(ModItems.SLEEVE.get())) {
            designed = sleeve.copy(); // already a finished sleeve; just restyle
        } else {
            // Promote a blank into a finished sleeve, carrying over any contained record.
            designed = new ItemStack(ModItems.SLEEVE.get(), 1);
            ItemStack contained = SleeveOps.containedRecord(sleeve);
            if (!contained.isEmpty()) {
                SleeveOps.setContainedRecord(designed, contained);
            }
        }
        designed.set(ModComponents.SLEEVE_DESIGN.get(), design);
        items.setStackInSlot(SLOT_SLEEVE, designed);

        if (Patterns.isDye(bgDye)) shrink(SLOT_BG_DYE);
        if (Patterns.isDye(patternDye)) shrink(SLOT_PATTERN_DYE);
        if (sticker != 0) shrink(SLOT_TEMPLATE);
    }

    private void shrink(int slot) {
        ItemStack s = items.getStackInSlot(slot);
        s.shrink(1);
        items.setStackInSlot(slot, s);
    }

    /** Bake the sleeve's design into a reusable stencil. Requires an empty (record-free) sleeve. */
    public void makeStencil() {
        ItemStack sleeve = items.getStackInSlot(SLOT_SLEEVE);
        SleeveDesign design = sleeve.get(ModComponents.SLEEVE_DESIGN.get());
        if (design == null || hasContainedRecord(sleeve)) return;
        if (!items.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;
        ItemStack stencil = new ItemStack(ModItems.SLEEVE_STENCIL.get(), 1);
        stencil.set(ModComponents.SLEEVE_DESIGN.get(), design);
        items.setStackInSlot(SLOT_OUTPUT, stencil);
        sleeve.shrink(1);
        items.setStackInSlot(SLOT_SLEEVE, sleeve);
    }

    private static boolean isSleeve(ItemStack stack) {
        return stack.is(ModItems.SLEEVE.get()) || stack.is(ModItems.SLEEVE_BLANK.get());
    }

    private static boolean hasContainedRecord(ItemStack sleeve) {
        return SleeveOps.hasContainedRecord(sleeve);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    // --- menu / persistence ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cdm.packaging_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PackagingMenu(id, inv, this);
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
