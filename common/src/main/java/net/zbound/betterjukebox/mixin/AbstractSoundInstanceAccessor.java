package net.zbound.betterjukebox.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// This is required to forcefully modify the protected fields
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    /**
     * Sets whether or not the sound fades as you move away from the source (and theoretically the different levels of fading).
     * However, setting the sound to relative renders this field pointless, as relative sounds are global.
     */
    @Accessor("attenuation")
    void setAttenuationType(SoundInstance.Attenuation attenuationType);

    /**
     * Sets a sound to be relative to the client (player), meaning it's effectively a global sound. Used for music/ambient sounds.
     * @param isRelative
     */
    @Accessor("relative")
    void setRelative(boolean isRelative);

    /**
     * Sets the actual volume of the sound (not some distance parameter in datapacks).
     * Could be useful for manual attenuation to limit the distance of a jukebox.
     * Maybe put inject it into the tick() function?
     * Setting above the default 4.0F for music discs won't increase the volume, so it's not going to blast out your ears if you do.
     * It looks like this is how Minecraft does it actually in SoundSystem#getAdjustedVolume(), where the volume is set to 0 to 1, also multiplied by sounds percent slider.
     *
     * This only works if you set it initially (at the top of SoundSystem#play()), not when it's already playing.
     * @param volume
     */
    @Accessor("volume")
    void setVolume(float volume);

    @Accessor("x")
    void setX(double x);

    @Accessor("y")
    void setY(double y);

    @Accessor("z")
    void setZ(double z);
}
