package com.cdm.registry;

import com.cdm.CDMMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

/**
 * Key for the single datapack {@link JukeboxSong} our discs reference, so a vanilla jukebox accepts
 * and "plays" them. Its registered sound is suppressed by the jukebox mixin, which instead emits the
 * disc's note sequence. Defined as data in data/cdm/jukebox_song/custom.json.
 */
public final class ModJukeboxSongs {
    private ModJukeboxSongs() {}

    public static final ResourceKey<JukeboxSong> CUSTOM =
            ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(CDMMod.MODID, "custom"));
}
