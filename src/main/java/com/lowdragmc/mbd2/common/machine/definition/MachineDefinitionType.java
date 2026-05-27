package com.lowdragmc.mbd2.common.machine.definition;

import lombok.Getter;

@Getter
public abstract class MachineDefinitionType<T extends MBDMachineDefinition> {
    public final String name;
    public final String folder;
    public final String fileSuffix;

    protected MachineDefinitionType(String name, String folder, String fileSuffix) {
        this.name = name;
        this.folder = folder;
        this.fileSuffix = fileSuffix;
    }

    public abstract T createDefinition();
}
