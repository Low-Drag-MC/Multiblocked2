package com.lowdragmc.mbd2.common.machine.definition.config;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
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
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import com.lowdragmc.mbd2.api.block.RotationState;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaOverflow;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@Accessors(fluent = true)
@Builder
@KJSBindings
public class ConfigBlockProperties implements IPersistedSerializable, IConfigurable {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class RenderTypes implements IPersistedSerializable {
        @Configurable(name = "config.block_properties.render_types.solid")
        private boolean solid;
        @Configurable(name = "config.block_properties.render_types.cutout")
        private boolean cutout = true;
        @Configurable(name = "config.block_properties.render_types.cutout_mipped")
        private boolean cutoutMipped;
        @Configurable(name = "config.block_properties.render_types.translucent")
        private boolean translucent;
    }

    @Configurable(name = "config.block_properties.render_types", subConfigurable = true, tips = {
            "config.block_properties.render_types.tooltip",
            "config.require_restart"})
    @Builder.Default
    private final RenderTypes renderTypes = new RenderTypes();

    @Configurable(name = "config.block_properties.use_ao", tips = "config.block_properties.use_ao.tooltip")
    @Builder.Default
    private boolean useAO = true;

    @Configurable(name = "config.block_properties.rotation_state", tips = {"config.block_properties.rotation_state.tooltip",
            "config.require_restart"})
    @Builder.Default
    private RotationState rotationState = RotationState.NON_Y_AXIS;

    @Configurable(name = "config.block_properties.has_collision", tips = {"config.block_properties.has_collision.tooltip",
            "config.require_restart"})
    @Builder.Default
    private boolean hasCollision = true;

    @Configurable(name = "config.block_properties.can_occlude", tips = {"config.block_properties.can_occlude.tooltip",
            "config.require_restart"})
    @Builder.Default
    private boolean canOcclude = true;

    @Configurable(name = "config.block_properties.ignited_by_lava", tips = {"config.block_properties.ignited_by_lava.tooltip",
            "config.require_restart"})
    @Builder.Default
    private boolean ignitedByLava = false;

    @Configurable(name = "config.block_properties.is_air", tips = {"config.block_properties.is_air.tooltip", "config.require_restart"})
    @Builder.Default
    private boolean isAir = false;

    @Configurable(name = "config.block_properties.is_suffocating", tips = {"config.block_properties.is_suffocating.tooltip",
            "config.require_restart"})
    @Builder.Default
    private boolean isSuffocating = false;

    @Configurable(name = "config.block_properties.emissive", tips = {"config.block_properties.emissive.tooltip",
            "config.require_restart"})
    @Builder.Default
    private boolean emissive = false;

    @Configurable(name = "config.block_properties.friction", tips = {"config.block_properties.friction.tooltip",
            "config.require_restart"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float friction = 0.6f;

    @Configurable(name = "config.block_properties.speed_factor", tips = {"config.block_properties.speed_factor.tooltip",
            "config.require_restart"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float speedFactor = 1.0f;

    @Configurable(name = "config.block_properties.jump_factor", tips = {"config.block_properties.jump_factor.tooltip",
            "config.require_restart"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float jumpFactor = 1.0f;

    @Configurable(name = "config.block_properties.destroy_time", tips = {"config.block_properties.destroy_time.tooltip",
            "config.require_restart"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float destroyTime = 1.5f;

    @Configurable(name = "config.block_properties.explosion_resistance", tips = {"config.block_properties.explosion_resistance.tooltip",
            "config.require_restart"})
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    @Builder.Default
    private float explosionResistance = 6.0f;

    @Configurable(name = "config.block_properties.block_sound", subConfigurable = true)
    @Builder.Default
    private BlockSound blockSound = new BlockSound();

    @Configurable(name = "config.block_properties.transparent", tips = "config.block_properties.transparent.tooltip")
    @Builder.Default
    private boolean transparent = false;

    @Configurable(name = "config.block_properties.force_solid", tips = {"config.block_properties.force_solid.tooltip", "config.require_restart"})
    @Builder.Default
    private boolean forceSolid = false;

    @Configurable(name = "config.block_properties.replaceable", tips = {"config.block_properties.replaceable.tooltip", "config.require_restart"})
    @Builder.Default
    private boolean replaceable = false;

    @Configurable(name = "config.block_properties.no_particle_on_break", tips = {"config.block_properties.no_particle_on_break.tooltip", "config.require_restart"})
    @Builder.Default
    private boolean noParticleOnBreak = false;

    @Configurable(name = "config.block_properties.can_be_waterlogged", tips = "config.block_properties.can_be_waterlogged.tooltip")
    @Builder.Default
    private boolean canBeWaterlogged = false;

    @Configurable(name = "config.block_properties.collision_shape_full_block", tips = "config.block_properties.collision_shape_full_block.tooltip")
    @Builder.Default
    private boolean collisionShapeFullBlock = false;

    public BlockBehaviour.Properties apply(StateMachine<?> stateMachine, BlockBehaviour.Properties properties) {
        if (forceSolid) {
            properties = properties.forceSolidOn();
        }
        if (replaceable) {
            properties = properties.replaceable();
        }
        if (noParticleOnBreak) {
            properties = properties.noTerrainParticles();
        }
        if (hasCollision) {
            properties = properties.noOcclusion();
        }
        if (!canOcclude) {
            properties = properties.noOcclusion();
        }
        if (ignitedByLava) {
            properties = properties.ignitedByLava();
        }
        if (isAir) {
            properties = properties.air();
        }
        if (isSuffocating) {
            properties = properties.isSuffocating((state, level, pos) -> true);
        }
        if (emissive) {
            properties = properties.emissiveRendering((state, level, pos) -> true);
        }
        properties = properties.friction(friction);
        properties = properties.speedFactor(speedFactor);
        properties = properties.jumpFactor(jumpFactor);
        properties = properties.destroyTime(destroyTime);
        properties = properties.explosionResistance(explosionResistance);
        properties = properties.sound(blockSound.createSoundType());
        // check dynamic shape
        VoxelShape shape = null;
        for (var state : stateMachine.states.values()) {
            var stateShape = state.getShape(Direction.NORTH);
            if (shape == null) {
                shape = stateShape;
            } else if (shape != stateShape) {
                properties.dynamicShape();
                break;
            }
        }
        if (shape != Shapes.block() || !shape.isEmpty()) {
            properties.dynamicShape();
        }
        return properties;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class BlockSound implements IPersistedSerializable, IConfigurable {
        @Configurable(name = "config.block_properties.block_sound.volume", tips = "config.require_restart")
        @ConfigNumber(range = {0, Float.MAX_VALUE})
        private float volumeIn = 1;
        @Configurable(name = "config.block_properties.block_sound.pitch", tips = "config.require_restart")
        @ConfigNumber(range = {0, Float.MAX_VALUE})
        private float pitchIn = 1;
        @Configurable(name = "config.block_properties.block_sound.break")
        private ResourceLocation breakSound = SoundEvents.STONE_BREAK.getLocation();
        @Persisted
        @Configurable(name = "config.block_properties.block_sound.step")
        @ConfigSearch(searchConfiguratorMethod = "searchSound")
        private ResourceLocation stepSound = SoundEvents.STONE_STEP.getLocation();
        @Persisted
        @Configurable(name = "config.block_properties.block_sound.place")
        @ConfigSearch(searchConfiguratorMethod = "searchSound")
        private ResourceLocation placeSound = SoundEvents.STONE_PLACE.getLocation();
        @Persisted
        @Configurable(name = "config.block_properties.block_sound.hit")
        @ConfigSearch(searchConfiguratorMethod = "searchSound")
        private ResourceLocation hitSound = SoundEvents.STONE_HIT.getLocation();
        @Persisted
        @Configurable(name = "config.block_properties.block_sound.fall")
        @ConfigSearch(searchConfiguratorMethod = "searchSound")
        private ResourceLocation fallSound = SoundEvents.STONE_FALL.getLocation();

        // runtime
        private SoundEvent breakSoundEvent;
        private SoundEvent stepSoundEvent;
        private SoundEvent placeSoundEvent;
        private SoundEvent hitSoundEvent;
        private SoundEvent fallSoundEvent;

        public DeferredSoundType createSoundType() {
            return new DeferredSoundType(1.0f, 1.0f,
                    this::getBreakSoundEvent,
                    this::getStepSoundEvent,
                    this::getPlaceSoundEvent,
                    this::getHitSoundEvent,
                    this::getFallSoundEvent);
        }

        public SoundEvent getBreakSoundEvent() {
            if (breakSoundEvent == null) {
                breakSoundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(breakSound)).orElse(SoundEvents.EMPTY);
            }
            return breakSoundEvent;
        }

        public SoundEvent getStepSoundEvent() {
            if (stepSoundEvent == null) {
                stepSoundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(stepSound)).orElse(SoundEvents.EMPTY);
            }
            return stepSoundEvent;
        }

        public SoundEvent getPlaceSoundEvent() {
            if (placeSoundEvent == null) {
                placeSoundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(placeSound)).orElse(SoundEvents.EMPTY);
            }
            return placeSoundEvent;
        }

        public SoundEvent getHitSoundEvent() {
            if (hitSoundEvent == null) {
                hitSoundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(hitSound)).orElse(SoundEvents.EMPTY);
            }
            return hitSoundEvent;
        }

        public SoundEvent getFallSoundEvent() {
            if (fallSoundEvent == null) {
                fallSoundEvent = Optional.ofNullable(BuiltInRegistries.SOUND_EVENT.get(fallSound)).orElse(SoundEvents.EMPTY);
            }
            return fallSoundEvent;
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
                                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, pitchIn));
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
}
