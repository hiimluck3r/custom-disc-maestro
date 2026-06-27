package com.cdm.audio;

import java.util.List;

import com.cdm.data.NoteSequence;
import com.cdm.data.RecordContent;
import com.cdm.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Plays a disc's {@link NoteSequence} through Minecraft's own note-block sounds. Driven by the jukebox
 * mixin once per game tick while our disc is in a vanilla jukebox; emits server-side so all nearby
 * players hear it positionally (exactly like a vanilla disc).
 */
public final class DiscPlayback {
    private DiscPlayback() {}

    /** Plays when a disc has no melody of its own, so a plain creative disc still makes sound. */
    public static final NoteSequence TEST_MELODY = new NoteSequence(45, List.of(
            new NoteSequence.Note(0, 0, 6), new NoteSequence.Note(5, 0, 10),
            new NoteSequence.Note(10, 0, 13), new NoteSequence.Note(15, 0, 18),
            new NoteSequence.Note(20, 0, 13), new NoteSequence.Note(25, 0, 10),
            new NoteSequence.Note(30, 0, 6), new NoteSequence.Note(40, 6, 18)));

    public static NoteSequence resolveMelody(ItemStack disc) {
        RecordContent content = disc.get(ModComponents.RECORD_CONTENT.get());
        if (content != null && content.sideA().notes().isPresent()) {
            NoteSequence seq = NoteSequenceValidator.sanitize(content.sideA().notes().get());
            if (!seq.notes().isEmpty()) return seq;
        }
        return TEST_MELODY;
    }

    /**
     * Emit every note scheduled at {@code tick}, degraded by groove {@code wear} (0 = pristine, 1 =
     * broken). Worn discs drop notes, detune, and quieten; a broken disc is heavily distorted. Server only.
     */
    public static void playTick(Level level, BlockPos pos, NoteSequence seq, int tick, float wear) {
        RandomSource rnd = level.getRandom();
        float dropChance = wear < 0.5F ? 0F : wear < 0.75F ? 0.07F : wear < 1.0F ? 0.18F : 0.42F;
        float detune = wear < 0.5F ? 0F : wear < 0.75F ? 0.02F : wear < 1.0F ? 0.06F : 0.20F;
        float volume = wear >= 1.0F ? 2.1F : 3.0F;
        for (NoteSequence.Note n : seq.notes()) {
            if (n.tick() != tick) continue;
            if (dropChance > 0F && rnd.nextFloat() < dropChance) continue; // worn groove skips
            float pitch = NoteInstruments.pitch(n.note());
            if (detune > 0F) {
                pitch *= 1.0F + (rnd.nextFloat() * 2.0F - 1.0F) * detune;
                pitch = Mth.clamp(pitch, 0.5F, 2.0F);
            }
            level.playSound(null, pos, NoteInstruments.sound(n.instrument()).value(),
                    SoundSource.RECORDS, volume, pitch);
        }
    }
}
