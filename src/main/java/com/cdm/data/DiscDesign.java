package com.cdm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Visual design of a PRESSED vinyl disc: the vinyl colour, the label colour and a label style preset.
 * A disc with NO {@code DiscDesign} component is a raw white "master" (lacquer/DMM) — colour and label
 * are only applied when the disc is pressed from a matrix. {@link #STYLE_COUNT} label presets exist.
 */
public record DiscDesign(int vinylColor, int labelColor, int style) {
    public static final int STYLE_COUNT = 4; // plain, stripes, ribbon, dots

    public static final DiscDesign DEFAULT = new DiscDesign(0x1A1A1A, 0xE8C84A, 0);

    public static DiscDesign sanitize(DiscDesign in) {
        return new DiscDesign(in.vinylColor() & 0xFFFFFF, in.labelColor() & 0xFFFFFF,
                Math.floorMod(in.style(), STYLE_COUNT));
    }

    public static final Codec<DiscDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("vinyl", 0x1A1A1A).forGetter(DiscDesign::vinylColor),
            Codec.INT.optionalFieldOf("label", 0xE8C84A).forGetter(DiscDesign::labelColor),
            Codec.INT.optionalFieldOf("style", 0).forGetter(DiscDesign::style)
    ).apply(i, DiscDesign::new));

    public static final StreamCodec<ByteBuf, DiscDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DiscDesign::vinylColor,
            ByteBufCodecs.INT, DiscDesign::labelColor,
            ByteBufCodecs.VAR_INT, DiscDesign::style,
            DiscDesign::new);
}
