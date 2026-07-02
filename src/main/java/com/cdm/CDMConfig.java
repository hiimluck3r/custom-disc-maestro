package com.cdm;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config (per-world, synced to clients, editable by the admin in
 * {@code <world>/serverconfig/cdm-server.toml} or via the in-game config screen).
 *
 * <p>The values are applied when an item is CREATED — the lathe, the bath and the press stamp the
 * configured maximum onto the item as a {@code minecraft:max_damage} component — so already-existing
 * items keep the limit they were made with.
 */
public final class CDMConfig {
    private CDMConfig() {}

    public static final ModConfigSpec SPEC;

    /** Jukebox plays a freshly cut master disc survives before its groove is fully worn. */
    public static final ModConfigSpec.IntValue MASTER_USES;
    /** Jukebox plays a pressed record survives before its groove is fully worn. */
    public static final ModConfigSpec.IntValue RECORD_USES;
    /** Records one galvanic matrix presses before it is spent. */
    public static final ModConfigSpec.IntValue MATRIX_USES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Durability of the disc-making pipeline. Sound distortion always kicks in at",
                        "50% / 75% / 100% of the configured wear, whatever the numbers are.")
                .push("uses");
        MASTER_USES = b.comment("Plays a freshly cut MASTER disc survives before it is fully worn.")
                .defineInRange("masterUses", 32, 1, 65536);
        RECORD_USES = b.comment("Plays a PRESSED record survives before it is fully worn.")
                .defineInRange("recordUses", 32, 1, 65536);
        MATRIX_USES = b.comment("Records one galvanic matrix can press before it is destroyed.")
                .defineInRange("matrixUses", 3, 1, 65536);
        b.pop();
        SPEC = b.build();
    }
}
