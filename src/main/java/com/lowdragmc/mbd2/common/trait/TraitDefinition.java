package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.syncdata.IAutoPersistedSerializable;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.gui.editor.machine.MachineTraitPanel;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public abstract class TraitDefinition implements IConfigurable, IAutoPersistedSerializable {
    public static CompoundTag serializeDefinition(TraitDefinition definition) {
        return definition.serializeNBT();
    }

    @Nullable
    public static TraitDefinition deserializeDefinition(CompoundTag tag) {
        var type = tag.getString("_type");
        var wrapper = MBDRegistries.TRAIT_DEFINITIONS.get(type);
        if (wrapper != null) {
            var definition = wrapper.creator().get();
            definition.deserializeNBT(tag);
            return definition;
        }
        return null;
    }

    @Getter @Setter
    @Configurable(name = "config.definition.trait.name")
    private String name = name();

    @Getter @Setter
    @Configurable(name = "config.definition.trait.priority", tips = "config.definition.trait.priority.tooltip")
    @NumberRange(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
    private int priority;

    /**
     * Create a capability trait for the machine.
     */
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
     * Additional rendering after world rendering in trait panel for definition settings.
     */
    @OnlyIn(Dist.CLIENT)
    public void renderAfterWorldInTraitPanel(MachineTraitPanel panel) {
    }
}
