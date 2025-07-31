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
    private static final int TICKS_TO_FULLY_FADE_OUT = 20;
    @Unique
    private static final int TICKS_TO_FULLY_FADE_IN = 40;
    @Unique
    private static final float MUSIC_VOLUME_PER_TICK_TO_FADE_OUT = 1f / TICKS_TO_FULLY_FADE_OUT;
    @Unique
    private static final float MUSIC_VOLUME_PER_TICK_TO_FADE_IN = 1f / TICKS_TO_FULLY_FADE_IN;
    /**
     * Used to track the original coordinates of jukebox sounds.
     * <p>
     * TODO: Fix the memory leak by somehow listening for when the SoundInstance is removed/stopped.
     */
    @Unique
    private static final Map<SoundInstance, Vec3> coordinates = new HashMap<>();
    private final SoundEngineWrapper wrapper = (SoundEngineWrapper) this;
    /**
     * You can safely assume that there'll only be one music track playing.
     * <p>
     * Since different music tracks have different volumes, multiply the existing volume by a factor of 0 to 1 to match that.
     * <p>
     * IMPORTANT: You'll run into a lot of issues if you try to forcefully modify the SoundInstance volume directly to keep track of the volume, because the MusicManager probably tries to change it back to normal, resulting in a max volume of 0.4 heading towards 0.01666667 indefinitely for example. That's why you need to keep track of the volume separately, not try to store it on the SoundInstance.
     * <p>
     * Start at 1 because music should be unaffected by default.
     */
    @Unique
    private float currentMusicVolumeFactor = 1;
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
        }
    }

    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void injectTick(boolean isPaused, CallbackInfo ci) {
        Multimap<SoundSource, SoundInstance> sounds = wrapper.getInstanceBySource();
        // Neither music/ambient/records are considered ticking sounds, so looping through ticking sounds is empty
        Collection<SoundInstance> records = sounds.get(SoundSource.RECORDS);
        long amountRecordsHearable = records.stream().filter(sound -> sound.getVolume() > 0).count();
        Vec3 playerPosition = wrapper.getListener().getTransform().position();

        if (amountRecordsHearable > 0) {
            betterJukebox$musicFadeOut();
        } else {
            betterJukebox$musicFadeIn();
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
    private void betterJukebox$setMusicVolumeAndHandlePausing() {
        if (!wrapper.isLoaded()) {
            return;
        }

        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
            SoundInstance sound = entry.getKey();

            if (sound.getSource() == SoundSource.MUSIC) {
                ChannelAccess.ChannelHandle sourceManager = entry.getValue();
                float maxVolume = sound.getVolume(); // Usually 1.0 for C418 music and 0.4 for newer music

                sourceManager.execute(source -> {
                    // Originally, unpausing the game briefly resumed the music before it got paused again, resulting in what effectively sounds like an audio glitch.
                    // The good news is that by making sure the volume is zero first, it effectively solves this issue by playing that brief audio clip at zero volume.
                    // Before, it would briefly unpause at volume 1.0 for example, whereas with this, it briefly unpauses at volume 0.0.
                    // Theoretically, the only time you might notice it is when rapidly pausing/unpausing as the music is fading out, but I can't seem to notice it, so it should be fine.
                    source.setVolume(wrapper.calculateAdjustedVolume(maxVolume * currentMusicVolumeFactor, SoundSource.MUSIC));

                    if (currentMusicVolumeFactor <= 0 && !wasMusicPaused) {
                        sourceManager.execute(Channel::pause);
                        wasMusicPaused = true;
                        //System.out.println("Pausing music...");
                    } else if (currentMusicVolumeFactor > 0 && wasMusicPaused) {
                        sourceManager.execute(Channel::unpause);
                        wasMusicPaused = false;
                        //System.out.println("Resuming music...");
                    }

                    //System.out.println("Music Volume Adjustment: " + maxVolume + " * " + currentMusicVolumeFactor + " = " + maxVolume * currentMusicVolumeFactor + " (" + wasMusicPaused + ")");
                });
            }
        }
    }

    /**
     * Ticking method call to fade out the background music if necessary.
     */
    @Unique
    private void betterJukebox$musicFadeOut() {
        if (currentMusicVolumeFactor > 0) {
            currentMusicVolumeFactor = Math.max(currentMusicVolumeFactor - MUSIC_VOLUME_PER_TICK_TO_FADE_OUT, 0);
            betterJukebox$setMusicVolumeAndHandlePausing();
        } else {
            // If you're not continuously pausing the music, it comes back automatically.
            // This is a separation branch/function to avoid unnecessary calculations.
            betterJukebox$pauseMusic();
        }
    }

    /**
     * Ticking method call to fade in the background music if necessary.
     */
    @Unique
    private void betterJukebox$musicFadeIn() {
        if (currentMusicVolumeFactor < 1) {
            currentMusicVolumeFactor = Math.min(currentMusicVolumeFactor + MUSIC_VOLUME_PER_TICK_TO_FADE_IN, 1);
            betterJukebox$setMusicVolumeAndHandlePausing();
        }
    }

    @Unique
    private void betterJukebox$pauseMusic() {
        if (!wrapper.isLoaded()) {
            return;
        }

        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : wrapper.getInstanceToChannel().entrySet()) {
            SoundInstance sound = entry.getKey();

            if (sound.getSource() == SoundSource.MUSIC) {
                entry.getValue().execute(Channel::pause);
                //System.out.println("Pausing music...");
            }
        }
    }
}
