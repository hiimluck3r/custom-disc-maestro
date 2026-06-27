package com.cdm.net;

import com.cdm.CDMMod;
import com.cdm.block.entity.CuttingLatheBlockEntity;
import com.cdm.data.DiscMeta;
import com.cdm.data.NoteSequence;
import com.cdm.menu.CuttingLatheMenu;

import com.cdm.registry.ModItems;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server "cut this melody onto a disc" request from the Cutting Lathe screen. The server
 * re-validates the menu, the slot contents, and (in the BE) the sequence + metadata before creating
 * the disc — nothing here is trusted.
 */
public record LatheRecordPayload(BlockPos pos, NoteSequence sequence, DiscMeta meta) implements CustomPacketPayload {
    public static final Type<LatheRecordPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "lathe_record"));

    public static final StreamCodec<ByteBuf, LatheRecordPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LatheRecordPayload::pos,
            NoteSequence.STREAM_CODEC, LatheRecordPayload::sequence,
            DiscMeta.STREAM_CODEC, LatheRecordPayload::meta,
            LatheRecordPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LatheRecordPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CuttingLatheMenu menu) || !menu.isAt(msg.pos())) return;
            if (!(player.level().getBlockEntity(msg.pos()) instanceof CuttingLatheBlockEntity be)) return;
            if (!be.stillValid(player)) return;

            ItemStack disc = CuttingLatheBlockEntity.buildDisc(msg.sequence(), msg.meta());
            if (disc.isEmpty()) return; // nothing recorded / failed validation

            if (!consumeBlank(player)) {
                player.displayClientMessage(Component.translatable("cdm.lathe.need_blank"), true);
                return;
            }
            if (!player.getInventory().add(disc)) {
                player.drop(disc, false);
            }
            player.level().playSound(null, msg.pos(), SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.BLOCKS, 0.6F, 1.2F);
        });
    }

    /** Remove one blank disc from the player's inventory; returns false if they have none. */
    private static boolean consumeBlank(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(ModItems.BLANK_DISC.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
