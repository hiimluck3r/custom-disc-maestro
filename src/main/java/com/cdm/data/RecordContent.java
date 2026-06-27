package com.cdm.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The audio "groove" carried by a record as it flows through the pipeline (master -> galvanic matrix ->
 * pressed record). Holds both sides plus the playback speed.
 *
 * <p>A {@link Side} references an {@link AudioRef} once the audio has been rendered/baked to PCM
 * (at cut time). Before that it may be empty (e.g. a master tape still pointing at a sequence).
 */
public record RecordContent(Side sideA, Side sideB, int rpm) {

    /**
     * One side of a record. Holds the note melody played through Minecraft's own note-block sounds.
     * The melody comes from the in-game sequencer or an imported {@code .nbs} file; either way it is
     * a validated, bounded {@link NoteSequence} stored directly on the item.
     */
    public record Side(Optional<NoteSequence> notes, String title) {
        public static final Side EMPTY = new Side(Optional.empty(), "");

        public static Side ofNotes(NoteSequence seq, String title) {
            return new Side(Optional.of(seq), title);
        }

        public boolean isEmpty() {
            return notes.isEmpty();
        }

        public static final Codec<Side> CODEC = RecordCodecBuilder.create(i -> i.group(
                NoteSequence.CODEC.optionalFieldOf("notes").forGetter(Side::notes),
                Codec.STRING.optionalFieldOf("title", "").forGetter(Side::title)
        ).apply(i, Side::new));

        public static final StreamCodec<ByteBuf, Side> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(NoteSequence.STREAM_CODEC), Side::notes,
                ByteBufCodecs.STRING_UTF8, Side::title,
                Side::new);
    }

    public static final RecordContent EMPTY = new RecordContent(Side.EMPTY, Side.EMPTY, 33);

    public static final Codec<RecordContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Side.CODEC.optionalFieldOf("side_a", Side.EMPTY).forGetter(RecordContent::sideA),
            Side.CODEC.optionalFieldOf("side_b", Side.EMPTY).forGetter(RecordContent::sideB),
            Codec.INT.optionalFieldOf("rpm", 33).forGetter(RecordContent::rpm)
    ).apply(i, RecordContent::new));

    public static final StreamCodec<ByteBuf, RecordContent> STREAM_CODEC = StreamCodec.composite(
            Side.STREAM_CODEC, RecordContent::sideA,
            Side.STREAM_CODEC, RecordContent::sideB,
            ByteBufCodecs.VAR_INT, RecordContent::rpm,
            RecordContent::new);
}
