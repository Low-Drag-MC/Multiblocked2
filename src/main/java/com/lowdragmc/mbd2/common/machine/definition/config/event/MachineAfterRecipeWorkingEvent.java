package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
//@LDLRegister(name = "MachineAfterRecipeWorkingEvent", group = "MachineEvent")
public class MachineAfterRecipeWorkingEvent extends MachineEvent {
    public final MBDRecipe recipe;

    public MachineAfterRecipeWorkingEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }
}
