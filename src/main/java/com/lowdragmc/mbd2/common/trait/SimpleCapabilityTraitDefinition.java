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

@Getter @Setter
public abstract class SimpleCapabilityTraitDefinition<T, C extends @Nullable Object> extends RecipeCapabilityTraitDefinition implements IUIProviderTrait, ICapabilityProviderTrait {
    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Override
    public abstract SimpleCapabilityTrait<T, C> createTrait(MBDMachine machine);

    @Override
    public void registerCapability(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
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

    @Nullable
    protected T getCapContent(MBDMachine machine, C context) {
        if (machine.getTraitByDefinition(this) instanceof SimpleCapabilityTrait<?, ?> trait) {
            var traitCast = (SimpleCapabilityTrait<T, C>) trait;
            return traitCast.getCapContent(traitCast.getCapabilityIO(context));
        }
        return null;
    }

    /**
     * Get the capability for auto registration.
     */
    public abstract BlockCapability<T, C> getCapability();

}
