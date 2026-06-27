package com.cdm.audio;

import java.util.ArrayList;
import java.util.List;

import com.cdm.data.NoteSequence;

/**
 * Hard bounds + sanitizer for any {@link NoteSequence} that crosses a trust boundary — i.e. anything
 * built from an imported {@code .nbs} file or received from a client packet. The server must run
 * every incoming sequence through {@link #sanitize} before storing it on an item, so a malicious or
 * corrupt source cannot bloat saves/network or push out-of-range values into playback.
 *
 * <p>This is purely defensive data validation: clamp ranges, drop garbage, cap counts. There is no
 * code execution path in a note sequence; the risks are DoS (huge counts) and out-of-range values.
 */
public final class NoteSequenceValidator {
    private NoteSequenceValidator() {}

    /** Max note events kept; extras are dropped. A very long song is well under this. */
    public static final int MAX_NOTES = 8192;
    /** Max length in game ticks (~20 min at 20 tps). */
    public static final int MAX_LENGTH_TICKS = 24_000;
    public static final int MAX_TITLE_LENGTH = 64;
    public static final int MAX_INSTRUMENT = 15; // note-block instruments 0..15
    public static final int MAX_NOTE = 24;

    /**
     * Returns a bounded, range-clamped copy safe to persist and play. Never throws on bad input:
     * out-of-range notes are clamped, negative-tick notes dropped, and the list capped to
     * {@link #MAX_NOTES}.
     */
    public static NoteSequence sanitize(NoteSequence in) {
        if (in == null || in.notes().isEmpty()) {
            return NoteSequence.EMPTY;
        }
        List<NoteSequence.Note> out = new ArrayList<>(Math.min(in.notes().size(), MAX_NOTES));
        int maxTick = 0;
        for (NoteSequence.Note n : in.notes()) {
            if (out.size() >= MAX_NOTES) break;
            int tick = n.tick();
            if (tick < 0 || tick > MAX_LENGTH_TICKS) continue; // drop out-of-window events
            int instrument = Math.floorMod(n.instrument(), MAX_INSTRUMENT + 1);
            int note = Math.max(0, Math.min(MAX_NOTE, n.note()));
            out.add(new NoteSequence.Note(tick, instrument, note));
            if (tick > maxTick) maxTick = tick;
        }
        int length = Math.max(0, Math.min(MAX_LENGTH_TICKS, Math.max(in.lengthTicks(), maxTick)));
        return new NoteSequence(length, List.copyOf(out));
    }

    /** Clamp a free-text title (record/side name) coming from an untrusted source. */
    public static String sanitizeTitle(String title) {
        return com.cdm.util.Sanitize.title(title, MAX_TITLE_LENGTH);
    }
}
