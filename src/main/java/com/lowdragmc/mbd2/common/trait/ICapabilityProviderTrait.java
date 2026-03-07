package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public interface ICapabilityProviderTrait {
    /**
     * Registers a capability for the specified machine definition during the provided event.
     *
     * @param definition the machine definition for which the capability is being registered
     * @param event the event context used to register capabilities
     */
    void registerCapability(MBDMachineDefinition definition, RegisterCapabilitiesEvent event);
}
