package net.zbound.betterjukebox.mixin.client;

import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MusicManager.class)
public class MusicManagerMixin {
    private final MusicManagerWrapper wrapper = (MusicManagerWrapper) this;

    @Inject(method = "tick", at = @At("HEAD"))
    private void injectTick(CallbackInfo ci) {
        //System.out.println("Current gain: " + wrapper.getCurrentGain());

        /*SoundInstance sound = wrapper.getCurrentMusic();
        if (sound != null && sound.getSound() != null) {
            System.out.println("Current music volume: " + sound.getVolume());
        }*/
    }

    @Inject(method = "fadePlaying", at = @At("HEAD"))
    private void injectFadePlaying(float volume, CallbackInfoReturnable<Boolean> cir) {
        System.out.println("Music fading towards: " + volume);
    }
}
