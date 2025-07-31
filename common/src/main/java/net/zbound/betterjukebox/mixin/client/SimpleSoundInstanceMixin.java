package net.zbound.betterjukebox.mixin.client;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.phys.Vec3;
import net.zbound.betterjukebox.util.SimpleSoundInstanceData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * General class that both music and records use, involves position
 */
@Mixin(SimpleSoundInstance.class)
public abstract class SimpleSoundInstanceMixin implements SimpleSoundInstanceData {
    @Unique
    private final Vec3 betterJukebox$originalCoordinates = Vec3.ZERO;

    @Inject(method = "forJukeboxSong", at = @At("RETURN"))
    private static void injectRecord(CallbackInfoReturnable<SimpleSoundInstance> cir) {
        SimpleSoundInstance sound = cir.getReturnValue();

        if (sound instanceof AbstractSoundInstanceWrapper modifiedSound) {
            if (sound instanceof SimpleSoundInstanceData soundData) {
                soundData.setOriginalCoordinates(new Vec3(sound.getX(), sound.getY(), sound.getZ()));
            }

            modifiedSound.setAttenuationType(SoundInstance.Attenuation.NONE);
            modifiedSound.setRelative(true);
            modifiedSound.setX(0);
            modifiedSound.setY(0);
            modifiedSound.setZ(0);
        }
    }

    @Override
    public Vec3 betterJukebox$getOriginalCoordinates() {
        return betterJukebox$originalCoordinates;
    }
}

// Attempt to convert "record" type to match "music" type params, because apparently music is also a PositionedSoundInstance?!
// volume = 1.0, notice how "relative = true" for music and ambient, could this be why?
// If not, then notice how stereo sounds are always non-directional while mono sounds are, which might be a property of the audio stream.
/*@Mixin(PositionedSoundInstance.class)
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
}*/
