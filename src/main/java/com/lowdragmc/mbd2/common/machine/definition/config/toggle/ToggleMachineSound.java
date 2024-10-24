package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.mbd2.client.MachineSound;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@Setter
public class ToggleMachineSound implements IToggleConfigurable {

    @Persisted
    protected boolean enable;
    @Persisted
    private ResourceLocation sound = SoundEvents.FURNACE_FIRE_CRACKLE.getLocation();
    @Configurable(name = "config.machine_sound.source", tips = "config.machine_sound.source.tooltip")
    private SoundSource soundSource = SoundSource.BLOCKS;
    @Configurable(name = "config.machine_sound.loop", tips = "config.machine_sound.loop.tooltip")
    private boolean loop = true;
    @Configurable(name = "config.machine_sound.delay", tips = "config.machine_sound.delay.tooltip")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    private int delay = 0;
    @Configurable(name = "config.machine_sound.volume", tips = "config.machine_sound.volume.tooltip")
    @NumberRange(range = {0, 100F})
    private float volume = 1.0F;
    @Configurable(name = "config.machine_sound.pitch", tips = "config.machine_sound.pitch.tooltip")
    @NumberRange(range = {0, 100F})
    private float pitch = 1.0F;

    // runtime
    private SoundEvent soundEvent;

    public SoundEvent getSoundEvent() {
        if (soundEvent == null) {
            soundEvent = Optional.ofNullable(ForgeRegistries.SOUND_EVENTS.getValue(sound)).orElse(SoundEvents.EMPTY);
        }
        return soundEvent;
    }

    @OnlyIn(Dist.CLIENT)
    public MachineSound createMachineSound(BlockPos pos, BooleanSupplier predicate) {
        return new MachineSound(getSoundEvent(), soundSource, predicate, pos, loop, delay, volume, pitch);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        father.addConfigurators(createSoundConfigurator("config.machine_sound.sound", this::setSound, this::getSound));
        IToggleConfigurable.super.buildConfigurator(father);
    }

    public Configurator createSoundConfigurator(String name, Consumer<ResourceLocation> setter, Supplier<ResourceLocation> getter) {
        return new SearchComponentConfigurator<>(name, getter, sound -> {
            setter.accept(sound);
            if (LDLib.isClient()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ForgeRegistries.SOUND_EVENTS.getValue(sound), pitch));
            }
        }, SoundEvents.STONE_PLACE.getLocation(), true, (word, find) -> {
            for (var key : ForgeRegistries.SOUND_EVENTS.getKeys()) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                if (key.toString().contains(word.toLowerCase())) {
                    find.accept(key);
                }
            }
        }, Object::toString);
    }
}
