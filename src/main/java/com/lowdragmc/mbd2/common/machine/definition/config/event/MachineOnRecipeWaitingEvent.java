package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
//@LDLRegister(name = "MachineOnRecipeWaitingEvent", group = "MachineEvent")
public class MachineOnRecipeWaitingEvent extends MachineEvent {
    public final MBDRecipe recipe;

    public MachineOnRecipeWaitingEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

}
