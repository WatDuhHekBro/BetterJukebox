package net.zbound.stereojukebox.client.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

// Music Fadeout:
// - Listen for pause, stop, and tick
// - Somehow lower volume of music source temporarily?

@Mixin(SoundSystem.class)
public abstract class JukeboxMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"))
    private void initInjected(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if(sound.getCategory() == SoundCategory.RECORDS) {
            if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                modifiedSound.setAttenuationType(SoundInstance.AttenuationType.NONE);
                modifiedSound.setRelative(true);
                //modifiedSound.setVolume(0.1f);
                //modifiedSound.setVolume(16f);

                // NOTE: This sets the sound's location to telport ~5 ~5 ~5 away from the player.
                // This always plays in the player's right ear.
                // Attenuation still works actually, it's just not really useful.
                // If you want to reduce the volume based on distance, you should do that separately.
                // You'd need to somehow get the player's position.

                //modifiedSound.setX(5);
                //modifiedSound.setY(5);
                //modifiedSound.setZ(5);

                // Make sure to set the relative position to the player's position if attenuation is enabled.
                modifiedSound.setX(0);
                modifiedSound.setY(0);
                modifiedSound.setZ(0);
            }

            //System.out.println(sound.getAttenuationType() + " / " + sound.isRelative() + " / " + sound.getX() + ", " + sound.getY() + " / " + sound.getZ() + " / ");
            //((SoundSystemWrapper) this).invokeStopSounds(null, SoundCategory.MUSIC);
            //pauseMusic();
        }
    }

    // Probably really inefficient to check every tick, but oh well :P
    // The music seems to come back if you only pause music the first tick (on play())
    // Maybe do the reverse in the menu via tick(bool paused) instead?
    // TODO: Use booleans to keep track of last amountRecords or check if music is already resumed (so you don't unnecessarily repeat that)
    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void injectTick(boolean isPaused, CallbackInfo ci) {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);
        Multimap<SoundCategory, SoundInstance> sounds = wrapper.getSounds();
        int amountRecords = sounds.get(SoundCategory.RECORDS).size();
        //int amountMusic = sounds.get(SoundCategory.MUSIC).size();
        //System.out.println("REC #" + amountRecords + ", MUS #" + amountMusic);
        //System.out.println("REC #" + amountRecords);

        if(!isPaused) {
            if (amountRecords > 0) {
                pauseMusic();
            } else {
                resumeMusic();
            }
        } else {
            resumeMusic();
        }

        // As it turns out, neither Records nor Music are considered ticking sounds
        /*List<TickableSoundInstance> tickingSounds = wrapper.getTickingSounds();
        int amount = 0;

        for(TickableSoundInstance tickableSoundInstance : tickingSounds) {
            if(tickableSoundInstance.getCategory() == SoundCategory.MUSIC) {
                amount++;
            }
        }

        System.out.println("Found " + amount + " matching ticking sounds.");*/
    }

    @Unique
    private void pauseMusic() {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);

        if (wrapper.isStarted()) {
            for(Map.Entry<SoundInstance, Channel.SourceManager> entry : wrapper.getSources().entrySet()) {
                SoundInstance sound = entry.getKey();
                if (sound.getCategory() == SoundCategory.MUSIC) {
                    entry.getValue().run(Source::pause);
                    /*if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                        modifiedSound.setVolume(0);
                    }*/
                }
            }
        }
    }

    @Unique
    private void resumeMusic() {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);

        if (wrapper.isStarted()) {
            for(Map.Entry<SoundInstance, Channel.SourceManager> entry : wrapper.getSources().entrySet()) {
                SoundInstance sound = entry.getKey();
                if (sound.getCategory() == SoundCategory.MUSIC) {
                    entry.getValue().run(Source::resume);
                }
            }
        }
    }
}

/*@Mixin(Source.class)
public abstract class JukeboxMixin {
    @Shadow public abstract boolean isPlaying();

    @Inject(method = "play", at = @At("HEAD"))
    private void playInjected(CallbackInfo ci) {
        System.out.println("PLAY TEST " + this.isPlaying());
    }
}*/

// SoundSystem or SoundManager?
// SoundSystem#play(Sound sound) -> SoundInstance -> AbstractSoundInstance -> PositionedSoundInstance
/*@Mixin(SoundSystem.class)
public abstract class JukeboxMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"))
    private void playInjected(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        // MUSIC // PositionedSoundInstance
        // RECORDS // PositionedSoundInstance
        System.out.println("PLAY TEST " + sound.getCategory() + " // " + sound.getClass().getSimpleName());
    }
}*/

// Attempt to convert "record" type to match "music" type params, because apparently music is also a PositionedSoundInstance?!
// volume = 1.0, notice how "relative = true" for music and ambient, could this be why?
// If not, then notice how stereo sounds are always non-directional while mono sounds are, which might be a property of the audio stream.
/*@Mixin(PositionedSoundInstance.class)
public abstract class JukeboxMixin {
    private SoundInstance.AttenuationType attenuationType;
    private boolean relative;
    private int x, y, z;

    // Cleaner would be to inject/overwrite PositionedSoundInstance.record(), as it's the dedicated method for it
    @Inject(method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/sound/SoundCategory;FFLnet/minecraft/util/math/random/Random;ZILnet/minecraft/client/sound/SoundInstance$AttenuationType;DDDZ)V", at = @At("TAIL"))
    private void initInjected(Identifier id, SoundCategory category, float volume, float pitch, Random random, boolean repeat, int repeatDelay, SoundInstance.AttenuationType attenuationType, double x, double y, double z, boolean relative, CallbackInfo ci) {
        if(category == SoundCategory.RECORDS) {
            //System.out.println(attenuationType);
            // This doesn't work because "this" doesn't actually reference the injected class
            // Nor can you seem to modify method parameters
            attenuationType = SoundInstance.AttenuationType.NONE;
            relative = true;
            x = 0;
            y = 0;
            z = 0;
            System.out.println(this);
        }
    }
}*/
