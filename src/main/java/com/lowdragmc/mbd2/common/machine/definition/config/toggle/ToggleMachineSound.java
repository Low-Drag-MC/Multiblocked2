package com.lowdragmc.mbd2.common.machine.definition.config.toggle;

import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.client.MachineSound;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;

@Getter
@Setter
public class ToggleMachineSound implements IToggleConfigurable {
    protected boolean enable;
    @Configurable(name = "config.machine_sound.sound")
    @ConfigSearch(searchConfiguratorMethod = "searchSound")
    private ResourceLocation sound = SoundEvents.FURNACE_FIRE_CRACKLE.getLocation();
    @Configurable(name = "config.machine_sound.source", tips = "config.machine_sound.source.tooltip")
    private SoundSource soundSource = SoundSource.BLOCKS;
    @Configurable(name = "config.machine_sound.loop", tips = "config.machine_sound.loop.tooltip")
    private boolean loop = true;
    @Configurable(name = "config.machine_sound.loop_with_shuffle", tips = "config.machine_sound.loop_with_shuffle.tooltip")
    private boolean loopWithShuffle;
    @Configurable(name = "config.machine_sound.delay", tips = "config.machine_sound.delay.tooltip")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int delay = 0;
    @Configurable(name = "config.machine_sound.volume", tips = "config.machine_sound.volume.tooltip")
    @ConfigNumber(range = {0, 100F})
    private float volume = 1.0F;
    @Configurable(name = "config.machine_sound.pitch", tips = "config.machine_sound.pitch.tooltip")
    @ConfigNumber(range = {0, 100F})
    private float pitch = 1.0F;

    // runtime
    private SoundEvent soundEvent;

    public SoundEvent getSoundEvent() {
        if (soundEvent == null) {
            soundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(sound)).orElse(SoundEvents.EMPTY);
        }
        return soundEvent;
    }

    @ConfigSetter(field = "sound")
    public void setSound(ResourceLocation sound) {
        this.sound = sound;
        soundEvent = null;
    }

    @OnlyIn(Dist.CLIENT)
    public MachineSound createMachineSound(BlockPos pos, BooleanSupplier predicate) {
        return new MachineSound(getSoundEvent(), soundSource, predicate, pos, loop, loopWithShuffle, delay, volume, pitch);
    }

    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchSound() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var wordLower = word.toLowerCase();
                for (var key : BuiltInRegistries.SOUND_EVENT.keySet()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (key.toString().contains(wordLower)) {
                        searchHandler.accept(key);
                    }
                }
            }

            @Override
            public @NotNull ResourceLocation defaultValue() {
                return SoundEvents.STONE_PLACE.getLocation();
            }

            @Override
            public @NotNull String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public UIElementProvider<ResourceLocation> candidateUIProvider() {
                return sound -> {
                    var container = new UIElement().layout(layout -> {
                        layout.setFlexDirection(YogaFlexDirection.ROW);
                        layout.setGap(YogaGutter.ALL, 2);
                        layout.setHeight(10);
                    }).addChildren();
                    var playButton = new Button().noText().addPreIcon(Icons.PLAY).layout(layout -> {
                        layout.setAspectRatio(1);
                        layout.setHeightPercent(100);
                    });
                    playButton.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                        var soundEvent = BuiltInRegistries.SOUND_EVENT.get(sound);
                        if (soundEvent != null) {
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitch));
                        }
                        e.stopImmediatePropagation();
                    });
                    var label = new TextElement()
                            .textStyle(style -> style.textWrap(TextWrap.HOVER_ROLL).textAlignVertical(Vertical.CENTER))
                            .setText(sound.toString()).layout(layout -> {
                                layout.setHeightPercent(100);
                                layout.setFlex(1);
                            }).setOverflow(YogaOverflow.HIDDEN);
                    return container.addChildren(label, playButton);
                };
            }
        };
    }
}
