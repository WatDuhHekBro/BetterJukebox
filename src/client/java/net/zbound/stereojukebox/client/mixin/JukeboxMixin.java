package net.zbound.stereojukebox.client.mixin;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*@Mixin(Source.class)
public abstract class JukeboxMixin {
    @Shadow public abstract boolean isPlaying();

    @Inject(method = "play", at = @At("HEAD"))
    private void playInjected(CallbackInfo ci) {
        System.out.println("PLAY TEST " + this.isPlaying());
    }
}*/

// SoundSystem or SoundManager?
// SoundSystem#play(Sound sound) -> SoundInstance -> AbstractSoundInstance -> PositionedSoundInstance
/*@Mixin(SoundSystem.class)
public abstract class JukeboxMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"))
    private void playInjected(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        // MUSIC // PositionedSoundInstance
        // RECORDS // PositionedSoundInstance
        System.out.println("PLAY TEST " + sound.getCategory() + " // " + sound.getClass().getSimpleName());
    }
}*/

// Attempt to convert "record" type to match "music" type params, because apparently music is also a PositionedSoundInstance?!
// volume = 1.0, notice how "relative = true" for music and ambient, could this be why?
// If not, then notice how stereo sounds are always non-directional while mono sounds are, which might be a property of the audio stream.
@Mixin(PositionedSoundInstance.class)
public abstract class JukeboxMixin {
    private SoundInstance.AttenuationType attenuationType;
    private boolean relative;
    private int x, y, z;

    // Cleaner would be to inject/overwrite PositionedSoundInstance.record(), as it's the dedicated method for it
    @Inject(method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/sound/SoundCategory;FFLnet/minecraft/util/math/random/Random;ZILnet/minecraft/client/sound/SoundInstance$AttenuationType;DDDZ)V", at = @At("TAIL"))
    private void initInjected(Identifier id, SoundCategory category, float volume, float pitch, Random random, boolean repeat, int repeatDelay, SoundInstance.AttenuationType attenuationType, double x, double y, double z, boolean relative, CallbackInfo ci) {
        if(category == SoundCategory.RECORDS) {
            //System.out.println(attenuationType);
            // This doesn't work because "this" doesn't actually reference the injected class
            // Nor can you seem to modify method parameters
            attenuationType = SoundInstance.AttenuationType.NONE;
            relative = true;
            x = 0;
            y = 0;
            z = 0;
            System.out.println(this);
        }
    }
}
