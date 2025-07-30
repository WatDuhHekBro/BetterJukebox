package net.zbound.stereojukebox.client.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(SoundSystem.class)
public interface SoundSystemWrapper {
    @Accessor
    boolean isStarted();

    @Accessor
    Map<SoundInstance, Channel.SourceManager> getSources();

    @Accessor
    Multimap<SoundCategory, SoundInstance> getSounds();

    @Accessor
    List<TickableSoundInstance> getTickingSounds();

    @Accessor
    SoundListener getListener();

    @Invoker("stopSounds")
    void invokeStopSounds(@Nullable Identifier id, @Nullable SoundCategory category);

    /**
     * Use this function to adjust the volume based on the user's volume slider for that category.
     *
     * @param volume A number between 0 to 1 (because it's volume * category, so 1 * 1 would be the max).
     * @param category The sound category to search the user's settings for.
     * @return The new volume adjusted for the user's saved setting for that sound category
     */
    @Invoker("getAdjustedVolume")
    float invokeGetAdjustedVolume(float volume, SoundCategory category);
}
