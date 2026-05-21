package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
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
import java.util.function.Supplier;

@Getter @Setter
public abstract class TraitDefinition implements IConfigurable, IPersistedSerializable, ILDLRegister<TraitDefinition, Supplier<TraitDefinition>> {
    @LDLRegister(name = "empty", registry = "mbd2:trait_definition_type", manual = true)
    private static final class EmptyTraitDefinition extends TraitDefinition {
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

    static Codec<TraitDefinition> createCodec() {
        return MBDRegistries.TRAIT_DEFINITION_TYPES.optionalCodec().dispatch(ILDLRegister::getRegistryHolderOptional,
                optional -> optional.map(holder ->
                                MapCodec.assumeMapUnsafe(PersistedParser.createCodec(holder.value())))
                        .orElseGet(() -> MapCodec.unit(EMPTY)));
    }

    public final static StreamCodec<RegistryFriendlyByteBuf, TraitDefinition> STREAM_CODEC = createStreamCodec();

    static StreamCodec<RegistryFriendlyByteBuf, TraitDefinition> createStreamCodec() {
        return MBDRegistries.TRAIT_DEFINITION_TYPES.streamCodec().dispatch(ILDLRegister::getRegistryHolder,
                holder -> PersistedParser.createStreamCodec(holder.value())
        );
    }

    @Configurable(name = "config.definition.trait.name")
    private String name = name();

    @Configurable(name = "config.definition.trait.priority", tips = "config.definition.trait.priority.tooltip")
    @ConfigNumber(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int priority;

    /**
     * Create a capability trait for the machine.
     */
    @Nullable
    public abstract ITrait createTrait(MBDMachine machine);

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
     * Get a fancy renderer for block entity.
     */
    public IRenderer getBESRenderer(IMachine machine) {
        return IRenderer.EMPTY;
    }

    @Override
    public String getTranslateKey() {
        return "config.definition.%s.%s.name".formatted(this.group(), this.name());
    }

    /**
     * Render editor highlights when this trait is selected in the trait editor view.
     */
    @OnlyIn(Dist.CLIENT)
    public void renderInEditor(MultiBufferSource bufferSource, float partialTicks) {
    }
}
