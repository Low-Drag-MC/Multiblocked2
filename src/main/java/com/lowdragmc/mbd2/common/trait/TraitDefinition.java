package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@Getter @Setter
public abstract class TraitDefinition implements IConfigurable, IPersistedSerializable {
    @LDLRegister(name = "empty", registry = "mbd2:trait_definition_type")
    public static final TraitDefinitionType<TraitDefinition> EMPTY_TYPE = new TraitDefinitionType<>("empty") {
        @Override
        public TraitDefinition createDefinition() {
            return new EmptyTraitDefinition();
        }
    };

    private static final class EmptyTraitDefinition extends TraitDefinition {
        @Override
        public TraitDefinitionType<?> type() {
            return EMPTY_TYPE;
        }

        @Override
        public @Nullable ITrait createTrait(MBDMachine machine) {
            return null;
        }

        @Override
        public IGuiTexture getIcon() {
            return IGuiTexture.EMPTY;
        }
    }

    public static final TraitDefinition EMPTY = new EmptyTraitDefinition();

    public final static Codec<TraitDefinition> CODEC = createCodec();

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Codec<TraitDefinition> createCodec() {
        return MBDRegistries.TRAIT_DEFINITION_TYPES.codec().dispatch(TraitDefinition::type,
                type -> MapCodec.assumeMapUnsafe(PersistedParser.createCodec(type::createDefinition)));
    }

    public final static StreamCodec<RegistryFriendlyByteBuf, TraitDefinition> STREAM_CODEC = createStreamCodec();

    static StreamCodec<RegistryFriendlyByteBuf, TraitDefinition> createStreamCodec() {
        return MBDRegistries.TRAIT_DEFINITION_TYPES.streamCodec().dispatch(TraitDefinition::type,
                type -> PersistedParser.createStreamCodec(type::createDefinition)
        );
    }

    @Configurable(name = "config.definition.trait.name")
    private String name = type().name;

    @Configurable(name = "config.definition.trait.priority", tips = "config.definition.trait.priority.tooltip")
    @ConfigNumber(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int priority;

    /**
     * Create a capability trait for the machine.
     */
    @Nullable
    public abstract ITrait createTrait(MBDMachine machine);

    /**
     * Get the unique registered type that creates and handles this definition kind.
     */
    public abstract TraitDefinitionType<?> type();

    /**
     * Get icon for editor.
     */
    public abstract IGuiTexture getIcon();

    /**
     * Whether machine can have multiple traits of this type.
     */
    public boolean allowMultiple() {
        return true;
    }

    /**
     * Whether this trait is mandatory for the machine it belongs to. Mandatory traits are
     * auto-included by their definition's {@code loadFactory()} and cannot be removed from
     * the trait editor UI.
     */
    public boolean isMandatory() {
        return false;
    }

    /**
     * Get a fancy renderer for block entity.
     */
    public IRenderer getBESRenderer(IMachine machine) {
        return IRenderer.EMPTY;
    }

    public String getTranslateKey() {
        return "config.definition.%s.%s.name".formatted(this.type().group, this.getName());
    }

    /**
     * Render editor highlights when this trait is selected in the trait editor view.
     */
    @OnlyIn(Dist.CLIENT)
    public void renderInEditor(MultiBufferSource bufferSource, float partialTicks) {
    }
}
