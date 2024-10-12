package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

@Getter
@LDLRegister(name = "MachinePlacedEvent", group = "MachineEvent")
public class MachinePlacedEvent extends MachineEvent {
    @GraphParameterGet
    public final LivingEntity player;
    @GraphParameterGet
    public final ItemStack itemStack;

    public MachinePlacedEvent(MBDMachine machine, LivingEntity player, ItemStack itemStack) {
        super(machine);
        this.player = player;
        this.itemStack = itemStack;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("player")).ifPresent(p -> p.setValue(player));
        Optional.ofNullable(exposedParameters.get("itemStack")).ifPresent(p -> p.setValue(itemStack));
    }
}
