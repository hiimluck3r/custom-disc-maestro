package com.cdm.nbs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cdm.audio.NoteSequenceValidator;
import com.cdm.data.NoteSequence;

/**
 * Defensive parser for the Note Block Studio (.nbs / OpenNBS) binary format, versions 0–5.
 *
 * <p>The input is an UNTRUSTED file chosen by the user, so every read is bounds-checked, all
 * attacker-controlled lengths are capped BEFORE any allocation, and the note/tick totals are bounded
 * to {@link NoteSequenceValidator} limits. Custom instruments (which in NBS reference external sound
 * files) are NEVER resolved as files — they are mapped to a safe vanilla instrument. There is no code
 * path that executes data; the only real risks are OOM / malformed reads, both handled here. Any
 * failure returns whatever was parsed so far (possibly empty) instead of throwing.
 */
public final class NbsParser {
    private NbsParser() {}

    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024; // 2 MiB hard cap on the file
    private static final int MAX_STRING = 8192;               // per-string byte cap
    private static final int MIN_KEY = 33;                    // NBS key 33 == note-block note 0

    /** NBS instrument index -> our NoteInstruments index (0..15). Custom (>=16) maps to harp (0). */
    private static final int[] NBS_TO_OURS = {0, 4, 1, 2, 3, 7, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15};

    public static Optional<NbsImport> parse(byte[] data) {
        if (data == null || data.length < 4 || data.length > MAX_FILE_BYTES) {
            return Optional.empty();
        }
        Cursor c = new Cursor(data);
        String name = "";
        String author = "";
        List<NoteSequence.Note> notes = new ArrayList<>();
        int maxTick = 0;
        try {
            int first = c.u16();
            int version;
            if (first == 0) {
                version = c.u8();
                c.u8();          // vanilla instrument count (unused)
                c.u16();         // song length (ticks) — recomputed from notes
            } else {
                version = 0;     // legacy format; `first` was the song length
            }
            c.u16();             // layer count

            name = c.string();
            author = c.string();
            c.string();          // original author
            c.string();          // description
            int tempoRaw = c.u16();           // ticks-per-second * 100
            double tps = tempoRaw <= 0 ? 10.0 : Math.max(1.0, Math.min(100.0, tempoRaw / 100.0));

            c.u8();  c.u8();  c.u8();          // auto-save, auto-save duration, time signature
            c.i32(); c.i32(); c.i32(); c.i32(); c.i32(); // stats (minutes, clicks x2, blocks +/-)
            c.string();                        // import file name
            if (version >= 4) {
                c.u8(); c.u8(); c.u16();        // loop, max loop count, loop start tick
            }

            // Note blocks: tick jumps, then per-tick layer jumps.
            int tick = -1;
            outer:
            while (true) {
                int tickJump = c.u16();
                if (tickJump == 0) break;
                tick += tickJump;
                int ourTick = (int) Math.round(tick * (20.0 / tps));
                if (tick < 0 || ourTick > NoteSequenceValidator.MAX_LENGTH_TICKS) break;

                while (true) {
                    int layerJump = c.u16();
                    if (layerJump == 0) break;
                    int instrument = c.u8();
                    int key = c.u8();
                    if (version >= 4) {
                        c.u8();   // velocity
                        c.u8();   // panning
                        c.u16();  // fine pitch
                    }
                    int ourInstr = instrument < NBS_TO_OURS.length ? NBS_TO_OURS[instrument] : 0;
                    int ourNote = Math.max(0, Math.min(24, key - MIN_KEY));
                    notes.add(new NoteSequence.Note(ourTick, ourInstr, ourNote));
                    if (ourTick > maxTick) maxTick = ourTick;
                    if (notes.size() >= NoteSequenceValidator.MAX_NOTES) break outer;
                }
            }
        } catch (RuntimeException eof) {
            // Truncated / malformed — keep whatever we read so far.
        }

        if (notes.isEmpty()) return Optional.empty();
        NoteSequence seq = NoteSequenceValidator.sanitize(new NoteSequence(maxTick + 4, notes));
        return Optional.of(new NbsImport(seq, clean(name), clean(author)));
    }

    private static String clean(String s) {
        return NoteSequenceValidator.sanitizeTitle(s);
    }

    /** Little-endian, bounds-checked cursor over the file bytes. Throws on underflow (caught above). */
    private static final class Cursor {
        private final byte[] b;
        private int p;

        Cursor(byte[] b) {
            this.b = b;
        }

        private void need(int n) {
            if (n < 0 || p + n > b.length) throw new IndexOutOfBoundsException();
        }

        int u8() {
            need(1);
            return b[p++] & 0xFF;
        }

        int u16() {
            need(2);
            int v = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
            p += 2;
            return v;
        }

        int i32() {
            need(4);
            int v = (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
            p += 4;
            return v;
        }

        String string() {
            int len = i32();
            if (len < 0 || len > MAX_STRING) throw new IndexOutOfBoundsException();
            need(len);
            String s = new String(b, p, len, StandardCharsets.UTF_8);
            p += len;
            return s;
        }
    }
}
