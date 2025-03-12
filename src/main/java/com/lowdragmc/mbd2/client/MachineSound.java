package com.lowdragmc.mbd2.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public class MachineSound extends AbstractTickableSoundInstance {

    public final boolean loop;
    public final boolean loopWithShuffle;
    public final BooleanSupplier predicate;

    public MachineSound(SoundEvent soundEvent, SoundSource soundSource, BooleanSupplier predicate, BlockPos pos, boolean loop, boolean loopWithShuffle, int delay, float volume, float pitch) {
        super(soundEvent, soundSource, Minecraft.getInstance().level.random);
        this.predicate = predicate;
        this.loop = loop;
        this.loopWithShuffle = loopWithShuffle;
        this.looping = loop && !loopWithShuffle;
        this.delay = delay;
        this.volume = volume;
        this.pitch = pitch;
        this.attenuation = Attenuation.LINEAR;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (!isStopped() && !predicate.getAsBoolean()) {
            release();
        }
    }

    public void release() {
        stop();
    }

    public void play() {
        Minecraft.getInstance().getSoundManager().play(this);
    }

}
