package net.zbound.stereojukebox.client.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public abstract class JukeboxDebugMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"))
    private void initInjected(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if(sound.getCategory() == SoundCategory.RECORDS) {
            if(sound instanceof AbstractSoundInstanceAccessor soundModifier) {
                soundModifier.setAttenuationType(SoundInstance.AttenuationType.NONE);
                soundModifier.setRelative(true);
                //soundModifier.setVolume(0.1f);
                //soundModifier.setVolume(16f);

                // NOTE: This sets the sound's location to telport ~5 ~5 ~5 away from the player.
                // This always plays in the player's right ear.
                // Attenuation still works actually, it's just not really useful.
                // If you want to reduce the volume based on distance, you should do that separately.
                // You'd need to somehow get the player's position.

                //soundModifier.setX(5);
                //soundModifier.setY(5);
                //soundModifier.setZ(5);

                // Make sure to set the relative position to the player's position if attenuation is enabled.
                soundModifier.setX(0);
                soundModifier.setY(0);
                soundModifier.setZ(0);
            }

            System.out.println(sound.getAttenuationType() + " / " + sound.isRelative() + " / " + sound.getX() + ", " + sound.getY() + " / " + sound.getZ() + " / ");
        }
    }
}
