package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.Map;
import java.util.Optional;


@Getter
@LDLRegister(name = "MachineStateChangedEvent", group = "MachineEvent")
public class MachineStateChangedEvent extends MachineEvent implements ICancellableEvent {
    @GraphParameterGet(displayName = "old state")
    public final String oldState;
    @GraphParameterGet(displayName = "new state")
    public final String newState;

    public MachineStateChangedEvent(MBDMachine machine, String oldState, String newState) {
        super(machine);
        this.oldState = oldState;
        this.newState = newState;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("oldState")).ifPresent(p -> p.setValue(oldState));
        Optional.ofNullable(exposedParameters.get("newState")).ifPresent(p -> p.setValue(newState));
    }
}
