package com.cdm.net;

import com.cdm.CDMMod;
import com.cdm.block.entity.CuttingLatheBlockEntity;
import com.cdm.data.DiscMeta;
import com.cdm.data.NoteSequence;
import com.cdm.menu.CuttingLatheMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server "cut this melody onto a disc" request from the Cutting Lathe screen. The server
 * re-validates the menu, the slot contents, and (in the BE) the sequence + metadata before consuming
 * the blank and stamping the master — nothing here is trusted.
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

            switch (be.cut(msg.sequence(), msg.meta())) {
                case OK -> player.level().playSound(null, msg.pos(), SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.BLOCKS, 0.6F, 1.2F);
                case NO_BLANK -> player.displayClientMessage(
                        Component.translatable("cdm.lathe.need_blank"), true);
                case OUTPUT_BLOCKED -> player.displayClientMessage(
                        Component.translatable("cdm.lathe.output_blocked"), true);
                case EMPTY_MELODY -> { } // nothing composed — ignore silently
            }
        });
    }
}
