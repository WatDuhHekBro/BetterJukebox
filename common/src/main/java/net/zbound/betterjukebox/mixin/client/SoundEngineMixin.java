package net.zbound.betterjukebox.mixin.client;

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

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Unique
    private static final double MIN_DISTANCE = 50.0;
    @Unique
    private static final double MAX_DISTANCE = 100.0;
    @Unique
    private static final double MIN_DISTANCE_SQUARED = MIN_DISTANCE * MIN_DISTANCE;
    @Unique
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    @Unique
    private static final double DIVISOR = MAX_DISTANCE_SQUARED - MIN_DISTANCE_SQUARED;
    @Unique
    private static final float MUSIC_FADE_VOLUME_PER_TICK = 0.025f; // Fades in 2 seconds
    /**
     * Used to track the original coordinates of jukebox sounds.
     * <p>
     * TODO: Fix the memory leak by somehow listening for when the SoundInstance is removed/stopped.
     */
    @Unique
    private static final Map<SoundInstance, Vec3> coordinates = new HashMap<>();
    @Unique
    private boolean wasMusicPaused = false;

    @Inject(method = "play", at = @At("HEAD"))
    private void injectPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound.getSource() == SoundSource.RECORDS && sound instanceof AbstractSoundInstanceWrapper modifiedSound) {
            modifiedSound.setRelative(true);
            // Attenuation still has an impact if the relative coordinates are far from the player, but isn't useful in this case.
            // Manual attenuation is done later on based on the player's distance.
            modifiedSound.setAttenuationType(SoundInstance.Attenuation.NONE);

            // This step is required because when you make a sound relative to the player, the coordinates become a relative offset to the player, meaning they still have an impact.
            // For example, a relative position of (5, 5, 5) will always play in the player's right ear, since the sound will basically "tp ~5 ~5 ~5".
            // Because of this, you must store the original coordinates separately.
            coordinates.put(sound, new Vec3(sound.getX(), sound.getY(), sound.getZ()));
            modifiedSound.setX(0);
            modifiedSound.setY(0);
            modifiedSound.setZ(0);

            //((SoundSystemWrapper) this).stopSounds(null, SoundCategory.MUSIC);
            betterJukebox$pauseMusic();
            wasMusicPaused = true;
        }
    }

    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void injectTick(boolean isPaused, CallbackInfo ci) {
        SoundEngineWrapper wrapper = ((SoundEngineWrapper) this);
        Multimap<SoundSource, SoundInstance> sounds = wrapper.getInstanceBySource();
        // Neither music/ambient/records are considered ticking sounds, so looping through ticking sounds is empty
        Collection<SoundInstance> records = sounds.get(SoundSource.RECORDS);
        long amountRecordsHearable = records.stream().filter(sound -> sound.getVolume() > 0).count();
        Vec3 playerPosition = wrapper.getListener().getTransform().position();

        if (amountRecordsHearable > 0) {
            // The music seems to come back automatically if you only pause music the first tick (on SoundEngine#play())
            // TODO: Figure out why this happens, inspect the MusicTracker.
            betterJukebox$pauseMusic();
            wasMusicPaused = true;

            //musicFadeOut();
        } else {
            if (wasMusicPaused) {
                betterJukebox$resumeMusic();
                wasMusicPaused = false;
            }

            //musicFadeIn();
        }

        // Dynamically set the volume of each music disc based on the player's distance from the corresponding jukebox.
        for (SoundInstance sound : records) {
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
            double distanceSquared = playerPosition.distanceToSqr(coordinates.get(sound));
            double calculatedVolume = (MAX_DISTANCE_SQUARED - distanceSquared) / DIVISOR;
            calculatedVolume = Math.clamp(calculatedVolume, 0, 1);
            float adjustedVolume = wrapper.calculateAdjustedVolume((float) calculatedVolume, SoundSource.RECORDS);

            wrapper.getInstanceToChannel().get(sound).execute(source -> source.setVolume(adjustedVolume));
            //System.out.println(distanceSquared + " ==> " + calculatedVolume + " ==> " + adjustedVolume);

            if (sound instanceof AbstractSoundInstanceWrapper modifiedSound) {
                modifiedSound.trackVolumeForReferenceOnly(adjustedVolume);
            }
        }
    }

    /**
     * Prevents music discs from being paused in the pause screen, treating them like background music.
     */
    @ModifyVariable(method = "pauseAllExcept", at = @At("HEAD"), argsOnly = true)
    private SoundSource[] injectIgnoredSoundCategories(SoundSource... categories) {
        SoundSource[] ignoredCategories = new SoundSource[categories.length + 1];

        System.arraycopy(categories, 0, ignoredCategories, 0, categories.length);
        ignoredCategories[categories.length] = SoundSource.RECORDS;

        return ignoredCategories;
    }

    @Unique
    private void betterJukebox$pauseMusic() {
        SoundEngineWrapper wrapper = ((SoundEngineWrapper) this);

        if (wrapper.isLoaded()) {
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();

                if (sound.getSource() == SoundSource.MUSIC) {
                    entry.getValue().execute(Channel::pause);

                    /*if(sound instanceof AbstractSoundInstanceAccessor modifiedSound) {
                        modifiedSound.trackVolumeForReferenceOnly(0);
                    }*/
                }
            }
        }
    }

    /**
     * Continuous method call to fade out the background music
     * <p>
     * TODO: It seems like this conflicts with MusicTracker's fade to volume, which is why the numbers are inconsistent
     * Potentially another HashMap?
     */
    @Unique
    private void betterJukebox$musicFadeOut() {
        SoundEngineWrapper wrapper = ((SoundEngineWrapper) this);

        if (wrapper.isLoaded()) {
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();
                float oldVolume = sound.getVolume();

                if (sound.getSource() == SoundSource.MUSIC && oldVolume > 0) {
                    float newVolume = Math.max(oldVolume - MUSIC_FADE_VOLUME_PER_TICK, 0);
                    ChannelAccess.ChannelHandle sourceManager = entry.getValue();

                    sourceManager.execute(source -> {
                        source.setVolume(newVolume);

                        if (newVolume <= 0) {
                            sourceManager.execute(Channel::pause);
                        }
                    });

                    if (sound instanceof AbstractSoundInstanceWrapper modifiedSound) {
                        modifiedSound.trackVolumeForReferenceOnly(newVolume);
                    }

                    //System.out.println("Fade Out: " + oldVolume + " ==> " + newVolume);
                }
            }
        }
    }

    @Unique
    private void betterJukebox$resumeMusic() {
        SoundEngineWrapper wrapper = ((SoundEngineWrapper) this);

        if (wrapper.isLoaded()) {
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();

                if (sound.getSource() == SoundSource.MUSIC) {
                    entry.getValue().execute(Channel::unpause);
                }
            }
        }
    }

    /**
     * Continuous method call to fade in the background music
     * <p>
     * TODO: It seems like this conflicts with MusicTracker's fade to volume, which is why the numbers are inconsistent
     * Potentially another HashMap?
     */
    @Unique
    private void betterJukebox$musicFadeIn() {
        SoundEngineWrapper wrapper = ((SoundEngineWrapper) this);

        if (wrapper.isLoaded()) {
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
                SoundInstance sound = entry.getKey();
                float oldVolume = sound.getVolume();

                if (sound.getSource() == SoundSource.MUSIC & oldVolume < 1) {
                    float newVolume = Math.min(oldVolume + MUSIC_FADE_VOLUME_PER_TICK, 1);
                    ChannelAccess.ChannelHandle sourceManager = entry.getValue();

                    sourceManager.execute(source -> {
                        source.setVolume(newVolume);
                        source.unpause();
                    });

                    if (sound instanceof AbstractSoundInstanceWrapper modifiedSound) {
                        modifiedSound.trackVolumeForReferenceOnly(newVolume);
                    }

                    //System.out.println("Fade In: " + oldVolume + " ==> " + newVolume);
                }
            }
        }
    }
}
