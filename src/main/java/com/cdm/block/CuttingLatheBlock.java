package com.cdm.block;

import com.cdm.block.entity.CuttingLatheBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Opens the Cutting Lathe GUI where players compose a melody and cut it onto a disc. */
public class CuttingLatheBlock extends WorktableBlock implements EntityBlock {
    /** True while a disc (blank or cut master) sits in the lathe, shown on the platter. */
    public static final BooleanProperty HAS_DISC = BlockStateProperties.HAS_RECORD;

    public CuttingLatheBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HAS_DISC, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_DISC);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingLatheBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CuttingLatheBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CuttingLatheBlockEntity be) {
            var handler = be.getItems();
            for (int i = 0; i < handler.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
