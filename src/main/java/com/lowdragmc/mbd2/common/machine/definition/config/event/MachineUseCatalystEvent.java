package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Map;
import java.util.Optional;

@Cancelable
@LDLRegister(name = "MachineUseCatalystEvent", group = "MachineEvent.Multiblock")
public class MachineUseCatalystEvent extends MachineEvent {
    @GraphParameterGet
    public final ItemStack catalyst;

    public MachineUseCatalystEvent(MBDMachine machine, ItemStack catalyst) {
        super(machine);
        this.catalyst = catalyst;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("catalyst")).ifPresent(p -> p.setValue(catalyst));
    }
}
