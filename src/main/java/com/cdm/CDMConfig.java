package com.cdm;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-authoritative configuration for CDM. Limits here guard the audio subsystem against
 * disk/memory abuse from user uploads (see plan §8). Expanded in later milestones.
 */
public class CDMConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_FILE_IMPORT = BUILDER
            .comment("Allow players to import arbitrary audio files from their computer onto records.")
            .define("allowFileImport", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_VOICECHAT = BUILDER
            .comment("If true, record playback is hard-disabled unless Simple Voice Chat is installed.")
            .define("requireVoicechat", false);

    public static final ModConfigSpec.IntValue MAX_UPLOAD_BYTES = BUILDER
            .comment("Maximum size (in bytes) of an imported audio file. Default 8 MiB.")
            .defineInRange("maxUploadBytes", 8 * 1024 * 1024, 64 * 1024, 64 * 1024 * 1024);

    public static final ModConfigSpec.IntValue PLAYBACK_RANGE = BUILDER
            .comment("Default audible range (blocks) of a record player.")
            .defineInRange("playbackRange", 24, 1, 128);

    static final ModConfigSpec SPEC = BUILDER.build();
}
