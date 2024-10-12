package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Map;
import java.util.Optional;

@Cancelable
@LDLRegister(name = "MachineCustomDataUpdateEvent", group = "MachineEvent")
public class MachineCustomDataUpdateEvent extends MachineEvent {
    @GraphParameterGet(displayName = "old data")
    public final CompoundTag oldValue;
    @GraphParameterGet(displayName = "new data")
    public final CompoundTag newValue;

    public MachineCustomDataUpdateEvent(MBDMachine machine, CompoundTag newValue, CompoundTag oldValue) {
        super(machine);
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("oldValue")).ifPresent(p -> p.setValue(oldValue));
        Optional.ofNullable(exposedParameters.get("newValue")).ifPresent(p -> p.setValue(newValue));
    }
}
