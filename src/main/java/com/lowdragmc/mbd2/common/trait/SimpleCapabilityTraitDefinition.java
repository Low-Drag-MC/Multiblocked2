package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public abstract class SimpleCapabilityTraitDefinition<T, C extends @Nullable Object> extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    public abstract static class Type<T, C extends @Nullable Object, D extends SimpleCapabilityTraitDefinition<T, C>> extends TraitDefinitionType<D> {
        protected Type(String name, String group) {
            super(name, group);
        }

        protected Type(String name) {
            super(name);
        }

        @Override
        public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    getCapability(),
                    definition.blockEntityType(),
                    (be, context) -> {
                        if (be instanceof IMachineBlockEntity machineBlockEntity && machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                            return getCapContent(machine, context);
                        }
                        return null;
                    });
        }

        @SuppressWarnings("unchecked")
        protected @Nullable T getCapContent(MBDMachine machine, C context) {
            List<T> contents = new ArrayList<>();
            for (var trait : machine.getAdditionalTraits()) {
                if (trait instanceof SimpleCapabilityTrait<?, ?> capabilityTrait && capabilityTrait.getDefinition().type() == this) {
                    var typedTrait = (SimpleCapabilityTrait<T, C>) capabilityTrait;
                    contents.add(typedTrait.getCapContent(typedTrait.getCapabilityIO(context)));
                }
            }
            return merge(contents);
        }

        protected abstract BlockCapability<T, C> getCapability();

        protected abstract @Nullable T merge(List<T> contents);
    }

    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Override
    public abstract SimpleCapabilityTrait<T, C> createTrait(MBDMachine machine);

    @Nullable
    protected T getCapContent(MBDMachine machine, C context) {
        if (machine.getTraitByDefinition(this) instanceof SimpleCapabilityTrait<?, ?> trait) {
            var traitCast = (SimpleCapabilityTrait<T, C>) trait;
            return traitCast.getCapContent(traitCast.getCapabilityIO(context));
        }
        return null;
    }
}
