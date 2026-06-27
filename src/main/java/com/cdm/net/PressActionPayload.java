package com.cdm.net;

import com.cdm.CDMMod;
import com.cdm.block.entity.RecordPressBlockEntity;
import com.cdm.menu.RecordPressMenu;

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

/** Client -> server "press one disc" request. The design is derived from the table's slots server-side. */
public record PressActionPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<PressActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "press_action"));

    public static final StreamCodec<ByteBuf, PressActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PressActionPayload::pos,
            PressActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PressActionPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof RecordPressMenu menu) || !menu.isAt(msg.pos())) return;
            if (!(player.level().getBlockEntity(msg.pos()) instanceof RecordPressBlockEntity be)) return;
            if (!be.stillValid(player)) return;
            if (be.press()) {
                player.level().playSound(null, msg.pos(), SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 0.7F, 0.8F);
            } else {
                player.displayClientMessage(Component.translatable("cdm.press.fail"), true);
            }
        });
    }
}
