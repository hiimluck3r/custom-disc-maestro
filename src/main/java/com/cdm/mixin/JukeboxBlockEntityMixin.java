package com.cdm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cdm.audio.DiscPlayback;
import com.cdm.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Makes our custom music discs play their note melody in a VANILLA jukebox:
 *  - {@link #cdm$suppressVanillaSong} stops the jukebox from firing the vanilla song sound for our
 *    disc (it sets the song state so ticking continues, but plays no sound). Vanilla discs are
 *    untouched.
 *  - {@link #cdm$emitNotes} emits the disc's notes each tick, server-side, positionally.
 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {

    @Redirect(
            method = "setTheItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/JukeboxSongPlayer;play(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/Holder;)V"))
    private void cdm$suppressVanillaSong(JukeboxSongPlayer player, LevelAccessor level, Holder<JukeboxSong> song) {
        JukeboxBlockEntity be = (JukeboxBlockEntity) (Object) this;
        ItemStack disc = be.getTheItem();
        if (disc.is(ModItems.MUSIC_DISC.get())) {
            // Each play wears the groove by one point (capped, never destroyed — a broken disc stays).
            if (level instanceof Level lvl && !lvl.isClientSide && disc.isDamageableItem()
                    && disc.getDamageValue() < disc.getMaxDamage()) {
                disc.setDamageValue(disc.getDamageValue() + 1);
                be.setChanged();
            }
            player.setSongWithoutPlaying(song, 0L); // tick the song (for our notes) but no vanilla sound
        } else {
            player.play(level, song);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void cdm$emitNotes(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity be, CallbackInfo ci) {
        if (level.isClientSide) return;
        JukeboxSongPlayer player = be.getSongPlayer();
        if (!player.isPlaying()) return;
        ItemStack disc = be.getTheItem();
        if (!disc.is(ModItems.MUSIC_DISC.get())) return;
        DiscPlayback.Prepared prepared = DiscPlayback.prepare(disc);
        int tick = (int) player.getTicksSinceSongStarted();
        if (tick >= prepared.lengthTicks()) {
            player.stop(level, state); // notes are done — stop now instead of waiting for the datapack length
            return;
        }
        float wear = disc.isDamageableItem() ? (float) disc.getDamageValue() / disc.getMaxDamage() : 0F;
        DiscPlayback.playTick(level, pos, prepared, tick, wear);
    }
}
