package com.cdm.nbs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cdm.data.NoteSequence;

/** Headless verification of the defensive .nbs parser: correct mapping + safety on hostile input. */
class NbsParserTest {

    /** Minimal new-format (v5) .nbs with a single note at tick 0. */
    private static byte[] oneNote(int instrument, int key) {
        Nbs w = new Nbs();
        w.s16(0);        // new-format marker
        w.u8(5);         // version
        w.u8(16);        // vanilla instrument count
        w.s16(0);        // song length
        w.s16(0);        // layer count
        w.str("Song");   // name
        w.str("Author"); // author
        w.str("");       // original author
        w.str("");       // description
        w.s16(1000);     // tempo = 10 TPS
        w.u8(0); w.u8(0); w.u8(4);                 // autosave, autosave dur, time signature
        w.i32(0); w.i32(0); w.i32(0); w.i32(0); w.i32(0); // stats
        w.str("");       // import file name
        w.u8(0); w.u8(0); w.s16(0);                // v4+: loop, max loop, loop start
        // notes
        w.s16(1);        // tick jump -> tick 0
        w.s16(1);        // layer jump -> layer 0
        w.u8(instrument);
        w.u8(key);
        w.u8(100); w.u8(100); w.s16(0);            // v4+: velocity, panning, fine pitch
        w.s16(0);        // end layers
        w.s16(0);        // end notes
        return w.toByteArray();
    }

    @Test
    void parsesSingleNoteWithCorrectMapping() {
        // NBS instrument 0 (harp) -> our 0; key 45 -> note 45-33 = 12.
        Optional<NbsImport> result = NbsParser.parse(oneNote(0, 45));
        assertTrue(result.isPresent(), "valid file should parse");
        NoteSequence seq = result.get().sequence();
        assertEquals(1, seq.notes().size());
        NoteSequence.Note n = seq.notes().get(0);
        assertEquals(0, n.tick());
        assertEquals(0, n.instrument());
        assertEquals(12, n.note());
        assertEquals("Song", result.get().songName());
        assertEquals("Author", result.get().songAuthor());
    }

    @Test
    void remapsNbsInstrumentOrder() {
        // NBS index 1 is "double bass" -> our bass index 4.
        NoteSequence.Note n = NbsParser.parse(oneNote(1, 45)).orElseThrow().sequence().notes().get(0);
        assertEquals(4, n.instrument());
    }

    @Test
    void customInstrumentMapsToSafeVanilla() {
        // A custom instrument index (>=16) must never be resolved as a file — it maps to harp (0).
        NoteSequence.Note n = NbsParser.parse(oneNote(200, 45)).orElseThrow().sequence().notes().get(0);
        assertEquals(0, n.instrument());
    }

    @Test
    void clampsOutOfRangeKey() {
        // key 200 -> 200-33 clamped to 24.
        NoteSequence.Note n = NbsParser.parse(oneNote(0, 200)).orElseThrow().sequence().notes().get(0);
        assertEquals(24, n.note());
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[NbsParser.MAX_FILE_BYTES + 1];
        assertTrue(NbsParser.parse(big).isEmpty());
    }

    @Test
    void rejectsTruncatedGarbageWithoutThrowing() {
        assertTrue(NbsParser.parse(new byte[] {1, 2, 3}).isEmpty());
        assertFalse(NbsParser.parse(null).isPresent());
    }

    @Test
    void declaredButAbsentStringLengthDoesNotAllocate() {
        // A header claiming a huge string length but with no bytes must fail closed, not OOM.
        Nbs w = new Nbs();
        w.s16(0); w.u8(5); w.u8(16); w.s16(0); w.s16(0);
        w.i32(Integer.MAX_VALUE); // name length = 2GB, but no data follows
        assertTrue(NbsParser.parse(w.toByteArray()).isEmpty());
    }

    /** Tiny little-endian writer mirroring the .nbs encoding. */
    private static final class Nbs {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        void u8(int v) { out.write(v & 0xFF); }

        void s16(int v) { out.write(v & 0xFF); out.write((v >> 8) & 0xFF); }

        void i32(int v) {
            out.write(v & 0xFF); out.write((v >> 8) & 0xFF);
            out.write((v >> 16) & 0xFF); out.write((v >> 24) & 0xFF);
        }

        void str(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            i32(b.length);
            out.writeBytes(b);
        }

        byte[] toByteArray() { return out.toByteArray(); }
    }
}
