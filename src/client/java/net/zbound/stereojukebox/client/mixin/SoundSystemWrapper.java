package net.zbound.stereojukebox.client.mixin;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Listener;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(SoundEngine.class)
public interface SoundSystemWrapper {
    @Accessor
    boolean isLoaded();

    @Accessor
    Map<SoundInstance, ChannelAccess.ChannelHandle> getInstanceToChannel();

    @Accessor
    Multimap<SoundSource, SoundInstance> getInstanceBySource();

    @Accessor
    List<TickableSoundInstance> getTickingSounds();

    @Accessor
    Listener getListener();

    @Invoker("stop")
    void invokeStopSounds(@Nullable ResourceLocation id, @Nullable SoundSource category);

    /**
     * Use this function to adjust the volume based on the user's volume slider for that category.
     *
     * @param volume A number between 0 to 1 (because it's volume * category, so 1 * 1 would be the max).
     * @param category The sound category to search the user's settings for.
     * @return The new volume adjusted for the user's saved setting for that sound category
     */
    @Invoker("calculateVolume")
    float invokeGetAdjustedVolume(float volume, SoundSource category);
}
