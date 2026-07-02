package com.cdm.block.entity;

import com.cdm.CDMConfig;
import com.cdm.block.RecordPressBlock;
import com.cdm.data.DiscDesign;
import com.cdm.data.DiscMeta;
import com.cdm.data.RecordContent;
import com.cdm.item.Patterns;
import com.cdm.menu.RecordPressMenu;
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
 * Backs the Record Press GUI. Presses a vinyl disc from a galvanic matrix + blank, colouring the vinyl
 * and label from dye items and stamping a pattern from a pattern template — dyes/template consumed on
 * press, and the matrix wears one use. Slots:
 * 0 matrix, 1 blank, 2 vinyl dye, 3 pattern template, 4 pattern dye, 5 output.
 */
public class RecordPressBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_MATRIX = 0;
    public static final int SLOT_BLANK = 1;
    public static final int SLOT_VINYL_DYE = 2;
    public static final int SLOT_TEMPLATE = 3;
    public static final int SLOT_PATTERN_DYE = 4;
    public static final int SLOT_OUTPUT = 5;

    // Vinyl is black by default (we press it from black "чернит" stock); the core/label is white.
    private static final int DEFAULT_VINYL = 0x141414;
    private static final int DEFAULT_LABEL = 0xFFFFFF;

    private final ItemStackHandler items = new ItemStackHandler(6) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == SLOT_OUTPUT) syncDiscOnPlaten();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_MATRIX -> stack.is(ModItems.MATRIX.get());
                case SLOT_BLANK -> stack.is(net.minecraft.world.item.Items.BLACKSTONE);
                case SLOT_VINYL_DYE, SLOT_PATTERN_DYE -> Patterns.isDye(stack);
                case SLOT_TEMPLATE -> Patterns.isTemplate(stack);
                default -> false;
            };
        }
    };

    public RecordPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECORD_PRESS.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Press one disc using the current slot contents (dyes + template consumed, matrix wears one use). */
    public boolean press() {
        ItemStack matrix = items.getStackInSlot(SLOT_MATRIX);
        ItemStack blank = items.getStackInSlot(SLOT_BLANK);
        RecordContent content = matrix.get(ModComponents.RECORD_CONTENT.get());
        if (content == null || !blank.is(net.minecraft.world.item.Items.BLACKSTONE) || !items.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return false;
        }
        ItemStack vinylDye = items.getStackInSlot(SLOT_VINYL_DYE);
        ItemStack template = items.getStackInSlot(SLOT_TEMPLATE);
        ItemStack patternDye = items.getStackInSlot(SLOT_PATTERN_DYE);

        int vinyl = Patterns.isDye(vinylDye) ? Patterns.dyeColor(vinylDye) : DEFAULT_VINYL;
        int label = Patterns.isDye(patternDye) ? Patterns.dyeColor(patternDye) : DEFAULT_LABEL;
        int style = Patterns.indexOf(template);

        ItemStack disc = new ItemStack(ModItems.MUSIC_DISC.get(), 1);
        disc.set(ModComponents.RECORD_CONTENT.get(), content);
        disc.set(DataComponents.MAX_DAMAGE, CDMConfig.RECORD_USES.get()); // admin-configured wear budget
        DiscMeta meta = matrix.get(ModComponents.DISC_META.get());
        if (meta != null) disc.set(ModComponents.DISC_META.get(), meta);
        disc.set(ModComponents.DISC_DESIGN.get(), DiscDesign.sanitize(new DiscDesign(vinyl, label, style)));
        // A matrix grown from a worn master stamps pre-worn records; BAKED_WEAR is a percentage so
        // it maps cleanly onto whatever maximum this record was configured with.
        Integer bakedWear = matrix.get(ModComponents.BAKED_WEAR.get());
        if (bakedWear != null && bakedWear > 0) {
            disc.setDamageValue(Math.min(disc.getMaxDamage(),
                    Math.round(disc.getMaxDamage() * bakedWear / 100.0F)));
        }

        items.setStackInSlot(SLOT_OUTPUT, disc);
        shrink(SLOT_BLANK);
        if (Patterns.isDye(vinylDye)) shrink(SLOT_VINYL_DYE);
        if (Patterns.isDye(patternDye)) shrink(SLOT_PATTERN_DYE);
        if (style != 0) shrink(SLOT_TEMPLATE);
        wearMatrix();
        return true;
    }

    /** Mirrors "a pressed record waits in the output" into HAS_DISC so it shows on the platen. */
    private void syncDiscOnPlaten() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(RecordPressBlock.HAS_DISC)) return;
        boolean hasDisc = !items.getStackInSlot(SLOT_OUTPUT).isEmpty();
        if (state.getValue(RecordPressBlock.HAS_DISC) != hasDisc) {
            level.setBlockAndUpdate(worldPosition, state.setValue(RecordPressBlock.HAS_DISC, hasDisc));
        }
    }

    /** The matrix wears one press; once its uses are spent it is destroyed. */
    private void wearMatrix() {
        ItemStack matrix = items.getStackInSlot(SLOT_MATRIX);
        if (!matrix.isDamageableItem()) return;
        int damage = matrix.getDamageValue() + 1;
        if (damage >= matrix.getMaxDamage()) {
            items.setStackInSlot(SLOT_MATRIX, ItemStack.EMPTY);
        } else {
            matrix.setDamageValue(damage);
            items.setStackInSlot(SLOT_MATRIX, matrix);
        }
    }

    private void shrink(int slot) {
        ItemStack s = items.getStackInSlot(slot);
        s.shrink(1);
        items.setStackInSlot(slot, s);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cdm.record_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RecordPressMenu(id, inv, this);
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
