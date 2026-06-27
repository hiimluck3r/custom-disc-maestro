package com.cdm.audio;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Maps note-block instrument indices (0..15) to vanilla note-block {@link SoundEvent}s and converts
 * note values to pitch, exactly like a vanilla note block. Used to play a {@code NoteSequence}
 * positionally through {@code level.playSound}, with no external dependency.
 */
public final class NoteInstruments {
    private NoteInstruments() {}

    @SuppressWarnings("unchecked")
    private static final Holder<SoundEvent>[] SOUNDS = new Holder[] {
            SoundEvents.NOTE_BLOCK_HARP,            // 0
            SoundEvents.NOTE_BLOCK_BASEDRUM,        // 1
            SoundEvents.NOTE_BLOCK_SNARE,           // 2
            SoundEvents.NOTE_BLOCK_HAT,             // 3
            SoundEvents.NOTE_BLOCK_BASS,            // 4
            SoundEvents.NOTE_BLOCK_FLUTE,           // 5
            SoundEvents.NOTE_BLOCK_BELL,            // 6
            SoundEvents.NOTE_BLOCK_GUITAR,          // 7
            SoundEvents.NOTE_BLOCK_CHIME,           // 8
            SoundEvents.NOTE_BLOCK_XYLOPHONE,       // 9
            SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE,  // 10
            SoundEvents.NOTE_BLOCK_COW_BELL,        // 11
            SoundEvents.NOTE_BLOCK_DIDGERIDOO,      // 12
            SoundEvents.NOTE_BLOCK_BIT,             // 13
            SoundEvents.NOTE_BLOCK_BANJO,           // 14
            SoundEvents.NOTE_BLOCK_PLING,           // 15
    };

    public static final int COUNT = SOUNDS.length;

    public static Holder<SoundEvent> sound(int instrument) {
        return SOUNDS[Math.floorMod(instrument, SOUNDS.length)];
    }

    /** Vanilla note-block pitch for a note value 0..24 (F#3..F#5, with 12 = base A). */
    public static float pitch(int note) {
        return (float) Math.pow(2.0D, (Math.max(0, Math.min(24, note)) - 12) / 12.0D);
    }
}
