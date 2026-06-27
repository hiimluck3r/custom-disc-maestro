package com.cdm.data;

import com.cdm.util.Sanitize;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Visual design of a record sleeve: a background colour, a sticker colour and a sticker preset overlay
 * (none / stripes / ribbon / dots), plus a title. Custom artwork is intentionally limited to safe
 * presets — no free-form image data is ever stored.
 */
public record SleeveDesign(int bgColor, int stickerColor, int sticker, String title) {
    public static final int STICKER_COUNT = 4; // 0 = none, then stripes / ribbon / dots
    public static final int MAX_TITLE = 48;

    public static final SleeveDesign DEFAULT = new SleeveDesign(0xE8E8E8, 0x303030, 0, "");

    public static SleeveDesign sanitize(SleeveDesign in) {
        return new SleeveDesign(in.bgColor() & 0xFFFFFF, in.stickerColor() & 0xFFFFFF,
                Math.floorMod(in.sticker(), STICKER_COUNT), Sanitize.title(in.title(), MAX_TITLE));
    }

    public static final Codec<SleeveDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("bg", 0xE8E8E8).forGetter(SleeveDesign::bgColor),
            Codec.INT.optionalFieldOf("sticker_color", 0x303030).forGetter(SleeveDesign::stickerColor),
            Codec.INT.optionalFieldOf("sticker", 0).forGetter(SleeveDesign::sticker),
            Codec.STRING.optionalFieldOf("title", "").forGetter(SleeveDesign::title)
    ).apply(i, SleeveDesign::new));

    public static final StreamCodec<ByteBuf, SleeveDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SleeveDesign::bgColor,
            ByteBufCodecs.INT, SleeveDesign::stickerColor,
            ByteBufCodecs.VAR_INT, SleeveDesign::sticker,
            ByteBufCodecs.STRING_UTF8, SleeveDesign::title,
            SleeveDesign::new);
}
