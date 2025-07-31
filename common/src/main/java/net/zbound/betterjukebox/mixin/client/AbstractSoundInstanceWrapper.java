package net.zbound.betterjukebox.mixin.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * This is required to forcefully modify the protected fields
 */
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceWrapper {
    /**
     * Sets whether or not the sound fades as you move away from the source.
     */
    @Accessor("attenuation")
    void setAttenuationType(SoundInstance.Attenuation attenuationType);

    /**
     * Sets a sound to be relative to the client (player), meaning it's effectively a global sound.
     * Used for music/ambient sounds.
     */
    @Accessor("relative")
    void setRelative(boolean isRelative);

    /**
     * Sets the actual volume of the sound (not some distance parameter in datapacks) if called before SoundEngine#play().
     * <p>
     * Does nothing if called afterwards, but is still useful for keep tracking of the previous volume set.
     * Channel#setVolume() is how you set it after SoundEngine#play(), but it doesn't track the last set volume as it deals with the audio library ABI.
     * <p>
     * It's useful for keeping track of if a jukebox is still hearable and current music fading volume.
     *
     * @param volume A number from 0 to 1 (numbers above 1 won't make any difference)
     */
    @Accessor("volume")
    void trackVolumeForReferenceOnly(float volume);

    @Accessor("x")
    void setX(double x);

    @Accessor("y")
    void setY(double y);

    @Accessor("z")
    void setZ(double z);
}
