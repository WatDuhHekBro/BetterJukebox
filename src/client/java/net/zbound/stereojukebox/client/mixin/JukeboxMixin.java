package net.zbound.stereojukebox.client.mixin;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Music Fadeout:
// - Listen for pause, stop, and tick
// - Somehow lower volume of music source temporarily?

@Mixin(SoundEngine.class)
public abstract class JukeboxMixin {
    private static final double MIN_DISTANCE = 50.0;
    private static final double MAX_DISTANCE = 100.0;
    private static final double MIN_DISTANCE_SQUARED = MIN_DISTANCE * MIN_DISTANCE;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final double DIVISOR = MAX_DISTANCE_SQUARED - MIN_DISTANCE_SQUARED;
    private static final float MUSIC_FADE_VOLUME_PER_TICK = 0.025f; // Fades in 2 seconds
    private static final HashMap<SoundInstance, Vec3> coordinates = new HashMap<>();

    @Inject(method = "play", at = @At("HEAD"))
    private void initInjected(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if(sound.getSource() == SoundSource.RECORDS) {
            coordinates.put(sound, new Vec3(sound.getX(), sound.getY(), sound.getZ()));

            if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                modifiedSound.setAttenuationType(SoundInstance.Attenuation.NONE);
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
                // IMPORTANT: Even with attenuation = none and relative = true, if the relative position isn't (0, 0, 0), then it'll favor one ear over the other.
                // Which means you must store the actual coordinates separately. Memory leak time?
                modifiedSound.setX(0);
                modifiedSound.setY(0);
                modifiedSound.setZ(0);
            }

            //System.out.println(sound.getAttenuationType() + " / " + sound.isRelative() + " / " + sound.getX() + ", " + sound.getY() + " / " + sound.getZ() + " / ");
            //((SoundSystemWrapper) this).invokeStopSounds(null, SoundCategory.MUSIC);
            pauseMusic();
        }
    }

    // Probably really inefficient to check every tick, but oh well :P
    // The music seems to come back if you only pause music the first tick (on play())
    // Maybe do the reverse in the menu via tick(bool paused) instead?
    // TODO: Use booleans to keep track of last amountRecords or check if music is already resumed (so you don't unnecessarily repeat that)
    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void injectTick(boolean isPaused, CallbackInfo ci) {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);
        Multimap<SoundSource, SoundInstance> sounds = wrapper.getInstanceBySource();
        Collection<SoundInstance> records = sounds.get(SoundSource.RECORDS);
        long amountRecordsHearable = records.stream().filter(sound -> sound.getVolume() > 0).count();
        //int amountMusic = sounds.get(SoundCategory.MUSIC).size();
        //System.out.println("REC #" + amountRecords + ", MUS #" + amountMusic);
        //System.out.println("REC #" + amountRecordsHearable);
        Vec3 transform = wrapper.getListener().getTransform().position();

        //if(!isPaused) {
        if (amountRecordsHearable > 0) {
            pauseMusic();
            //musicFadeOut();
        } else {
            resumeMusic();
            //musicFadeIn();
        }
        //} else {
            //resumeMusic();
        //}

        // Dynamically set the volume based on the player's distance for each music disc
        // This must be outside of the if conditions or else once a music disc is no longer hearable, it won't resume.
        for(SoundInstance sound : records) {
            double distanceSquared = transform.distanceToSqr(coordinates.get(sound));

            /*
             * Distance
             * --------
             * Let's say a max distance of 40 blocks and a min distance of 20 blocks
             * 0-20 blocks distance is volume 1 and 40+ blocks is volume 0
             * 0-400 distanceSquared is volume 1 and 1600+ blocks is volume 0
             *
             * 1600 - 1681 < 0
             *
             * 1600 - 1600 = 0
             * - 0 / 1600 = 0
             * - 0 / 1200 = 0
             *
             * 1600 - 1521 = 79
             * - 79 / 1600 = 0.049
             * - 79 / 1200 = 0.06
             *
             * 1600 - 900 = 700
             * - 700 / 1200 = 0.58 (midpoint is slightly above half volume)
             *
             * 1600 - 400 = 1200
             * - 1200 / 1600 = 0.75 (doesn't take min into account)
             * - 1200 / 1200 = 1
             *
             * 1600 - 1 = 1599
             * - 1599 / 1600 = 0.99
             * - 1599 / 1200 > 1
             *
             * 1600 - 0 = 1600
             * - 1600 / 1600 = 1
             * - 1600 / 1200 > 1
             */

            double calculatedVolume = (MAX_DISTANCE_SQUARED - distanceSquared) / DIVISOR;
            calculatedVolume = Math.clamp(calculatedVolume, 0, 1);
            //calculatedVolume = 1;
            float adjustedVolume = wrapper.invokeGetAdjustedVolume((float) calculatedVolume, SoundSource.RECORDS);

            wrapper.getInstanceToChannel().get(sound).execute(source -> source.setVolume(adjustedVolume));
            //System.out.println(distanceSquared + " ==> " + calculatedVolume + " ==> " + adjustedVolume);

            // This doesn't actually modify the volume (once playing).
            // However, it's an easy way to later check if this music disc sound is still hearable.
            if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                modifiedSound.setVolume(adjustedVolume);
            }
        }

        // Disregard forward and up, transform.position() is exactly what you're looking for
        // The player's position isn't under Player, it's the Camera object
        //System.out.println(transform.position() + " + " + transform.forward() + " + " + transform.up());

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

        if (wrapper.isLoaded()) {
            for(Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();

                if (sound.getSource() == SoundSource.MUSIC) {
                    entry.getValue().execute(Channel::pause);

                    // Used to track the volume manually
                    /*if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                        modifiedSound.setVolume(0);
                    }*/
                }
            }
        }
    }

    /**
     * Continuous method call to fade out the background music
     *
     * TODO: It seems like this conflicts with MusicTracker's fade to volume, which is why the numbers are inconsistent
     * Potentially another HashMap?
     */
    @Unique
    private void musicFadeOut() {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);

        if (wrapper.isLoaded()) {
            for(Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();
                float oldVolume = sound.getVolume();

                if (sound.getSource() == SoundSource.MUSIC && oldVolume > 0) {
                    float newVolume = Math.max(oldVolume - MUSIC_FADE_VOLUME_PER_TICK, 0);
                    ChannelAccess.ChannelHandle sourceManager = entry.getValue();

                    sourceManager.execute(source -> {
                        source.setVolume(newVolume);

                        if(newVolume <= 0) {
                            sourceManager.execute(Channel::pause);
                        }
                    });

                    // Used to track the volume manually
                    if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                        modifiedSound.setVolume(newVolume);
                    }

                    //System.out.println("Fade Out: " + oldVolume + " ==> " + newVolume);
                }
            }
        }
    }

    @Unique
    private void resumeMusic() {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);

        if (wrapper.isLoaded()) {
            for(Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();

                if (sound.getSource() == SoundSource.MUSIC) {
                    entry.getValue().execute(Channel::unpause);
                }
            }
        }
    }

    /**
     * Continuous method call to fade in the background music
     *
     * TODO: It seems like this conflicts with MusicTracker's fade to volume, which is why the numbers are inconsistent
     * Potentially another HashMap?
     */
    @Unique
    private void musicFadeIn() {
        SoundSystemWrapper wrapper = ((SoundSystemWrapper) this);

        if (wrapper.isLoaded()) {
            for(Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();
                float oldVolume = sound.getVolume();

                if (sound.getSource() == SoundSource.MUSIC & oldVolume < 1) {
                    float newVolume = Math.min(oldVolume + MUSIC_FADE_VOLUME_PER_TICK, 1);
                    ChannelAccess.ChannelHandle sourceManager = entry.getValue();

                    sourceManager.execute(source -> {
                        source.setVolume(newVolume);
                        source.unpause();
                    });

                    // Used to track the volume manually
                    if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                        modifiedSound.setVolume(newVolume);
                    }

                    //System.out.println("Fade In: " + oldVolume + " ==> " + newVolume);
                }
            }
        }
    }

    /**
     * Prevents music discs from being paused in the pause screen, treating them like background music
     */
    @ModifyVariable(method = "pauseAllExcept", at = @At("HEAD"), argsOnly = true)
    private SoundSource[] injectIgnoredSoundCategories(SoundSource... categories) {
        SoundSource[] ignoredCategories = new SoundSource[categories.length + 1];

        System.arraycopy(categories, 0, ignoredCategories, 0, categories.length);
        ignoredCategories[categories.length] = SoundSource.RECORDS;

        return ignoredCategories;
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
