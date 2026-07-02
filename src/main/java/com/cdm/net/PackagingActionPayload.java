package com.cdm.net;

import com.cdm.CDMMod;
import com.cdm.block.entity.PackagingTableBlockEntity;
import com.cdm.menu.PackagingMenu;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server request from the Packaging Table. Colours/pattern are taken from the table's dye and
 * template slots server-side; only the free-text title travels with the packet. The server verifies the
 * player has this table open before mutating anything.
 */
public record PackagingActionPayload(BlockPos pos, int action, String title) implements CustomPacketPayload {
    public static final int APPLY_DESIGN = 0;
    public static final int MAKE_STENCIL = 3; // 1/2 were insert/extract, retired for RMB packaging

    public static final Type<PackagingActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "packaging_action"));

    public static final StreamCodec<ByteBuf, PackagingActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PackagingActionPayload::pos,
            ByteBufCodecs.VAR_INT, PackagingActionPayload::action,
            ByteBufCodecs.STRING_UTF8, PackagingActionPayload::title,
            PackagingActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PackagingActionPayload msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof PackagingMenu menu) || !menu.isAt(msg.pos())) return;
            if (!(player.level().getBlockEntity(msg.pos()) instanceof PackagingTableBlockEntity be)) return;
            if (!be.stillValid(player)) return;

            switch (msg.action()) {
                case APPLY_DESIGN -> be.applyDesign(msg.title());
                case MAKE_STENCIL -> be.makeStencil();
                default -> { }
            }
        });
    }
}
