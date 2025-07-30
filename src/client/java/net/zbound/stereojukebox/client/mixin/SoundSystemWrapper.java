package net.zbound.stereojukebox.client.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
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

    @Invoker("stopSounds")
    void invokeStopSounds(@Nullable Identifier id, @Nullable SoundCategory category);
}
