package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterSet;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@LDLRegister(name = "MachineDropsEvent", group = "MachineEvent")
public class MachineDropsEvent extends MachineEvent {
    @GraphParameterGet
    public final Entity entity;
    @GraphParameterGet(identity = "drops.in")
    @GraphParameterSet(identity = "drops.out")
    public List<ItemStack> drops;

    public MachineDropsEvent(MBDMachine machine, Entity entity, List<ItemStack> drops) {
        super(machine);
        this.entity = entity;
        this.drops = drops;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("entity")).ifPresent(p -> p.setValue(entity));
        Optional.ofNullable(exposedParameters.get("drops.in")).ifPresent(p -> p.setValue(drops));
    }

    @Override
    public void gatherParameters(Map<String, ExposedParameter> exposedParameters) {
        super.gatherParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("drops.out")).ifPresent(p -> {
            if (p.getValue() instanceof List list) {
                if (list.isEmpty()) {
                    drops.clear();
                } else {
                    drops.clear();
                    for (Object o : list) {
                        if (o instanceof ItemStack itemStack) {
                            drops.add(itemStack);
                        }
                    }
                }
            }
        });
    }
}
