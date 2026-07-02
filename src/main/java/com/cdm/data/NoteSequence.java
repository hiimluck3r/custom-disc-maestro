package com.cdm.data;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A composed melody, stored compactly on a record and played back through Minecraft's own note-block
 * sounds (positional, multiplayer, no external dependency) — exactly like vanilla note blocks.
 */
public record NoteSequence(int lengthTicks, List<Note> notes) {
    public static final NoteSequence EMPTY = new NoteSequence(0, List.of());

    /**
     * A single note event.
     *
     * @param tick       when it plays, in game ticks from the start of the side
     * @param instrument note-block instrument index 0..15 (see {@code NoteInstruments})
     * @param note       note-block pitch 0..24 (vanilla pitch = 2^((note-12)/12))
     */
    public record Note(int tick, int instrument, int note) {
        public static final Codec<Note> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("tick").forGetter(Note::tick),
                Codec.INT.fieldOf("instrument").forGetter(Note::instrument),
                Codec.INT.fieldOf("note").forGetter(Note::note)
        ).apply(i, Note::new));

        public static final StreamCodec<ByteBuf, Note> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Note::tick,
                ByteBufCodecs.VAR_INT, Note::instrument,
                ByteBufCodecs.VAR_INT, Note::note,
                Note::new);
    }

    public static final Codec<NoteSequence> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("length_ticks", 0).forGetter(NoteSequence::lengthTicks),
            Note.CODEC.listOf().optionalFieldOf("notes", List.of()).forGetter(NoteSequence::notes)
    ).apply(i, NoteSequence::new));

    /** Hard wire cap so a crafted packet cannot allocate an unbounded note list (DoS guard). */
    public static final int MAX_WIRE_NOTES = 8192;

    public static final StreamCodec<ByteBuf, NoteSequence> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NoteSequence::lengthTicks,
            Note.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_WIRE_NOTES)), NoteSequence::notes,
            NoteSequence::new);
}
