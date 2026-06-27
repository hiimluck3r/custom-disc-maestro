package com.cdm.data;

import java.util.Optional;

import com.cdm.util.Sanitize;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Human-authored metadata for a music disc: composition title, author, and optional album. Edited in
 * the Cutting Lathe GUI, shown in the tooltip and as the jukebox "Now Playing" text.
 */
public record DiscMeta(String title, String author, Optional<String> album) {
    public static final int MAX_LEN = 48;
    public static final DiscMeta EMPTY = new DiscMeta("", "", Optional.empty());

    public static final Codec<DiscMeta> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("title", "").forGetter(DiscMeta::title),
            Codec.STRING.optionalFieldOf("author", "").forGetter(DiscMeta::author),
            Codec.STRING.optionalFieldOf("album").forGetter(DiscMeta::album)
    ).apply(i, DiscMeta::new));

    public static final StreamCodec<ByteBuf, DiscMeta> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DiscMeta::title,
            ByteBufCodecs.STRING_UTF8, DiscMeta::author,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), DiscMeta::album,
            DiscMeta::new);

    /** Clamp all fields; used whenever metadata crosses the client -> server boundary. */
    public static DiscMeta sanitize(DiscMeta in) {
        if (in == null) return EMPTY;
        Optional<String> album = in.album()
                .map(a -> Sanitize.title(a, MAX_LEN))
                .filter(a -> !a.isBlank());
        return new DiscMeta(Sanitize.title(in.title(), MAX_LEN), Sanitize.title(in.author(), MAX_LEN), album);
    }

    public boolean isEmpty() {
        return title.isBlank() && author.isBlank() && album.isEmpty();
    }
}
