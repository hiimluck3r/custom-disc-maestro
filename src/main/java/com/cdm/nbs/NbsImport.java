package com.cdm.nbs;

import com.cdm.data.NoteSequence;

/** Result of parsing a .nbs file: the converted melody plus the song's name/author (best-effort). */
public record NbsImport(NoteSequence sequence, String songName, String songAuthor) {}
