package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import lombok.Getter;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Getter
public abstract class TraitDefinitionType<T extends TraitDefinition> {
    public final String name;
    public final String group;

    protected TraitDefinitionType(String name, String group) {
        this.name = name;
        this.group = group;
    }

    protected TraitDefinitionType(String name) {
        this(name, "");
    }

    public abstract T createDefinition();

    public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
    }
}
