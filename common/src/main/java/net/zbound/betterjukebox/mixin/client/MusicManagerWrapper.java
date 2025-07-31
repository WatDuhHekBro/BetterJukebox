package net.zbound.betterjukebox.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicManagerWrapper {
    @Accessor
    float getCurrentGain();

    @Accessor
    SoundInstance getCurrentMusic();
}
