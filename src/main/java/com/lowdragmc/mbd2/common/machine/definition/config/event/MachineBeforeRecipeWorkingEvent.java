package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
//@LDLRegister(name = "MachineBeforeRecipeWorkingEvent", group = "MachineEvent")
public class MachineBeforeRecipeWorkingEvent extends MachineEvent implements ICancellableEvent {
    public final MBDRecipe recipe;

    public MachineBeforeRecipeWorkingEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }
}
