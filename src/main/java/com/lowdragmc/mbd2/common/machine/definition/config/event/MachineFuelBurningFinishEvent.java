package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

import javax.annotation.Nullable;

@Getter
//@LDLRegister(name = "MachineFuelBurningFinishEvent", group = "MachineEvent")
public class MachineFuelBurningFinishEvent extends MachineEvent {
    @Nullable
    public final MBDRecipe recipe;

    public MachineFuelBurningFinishEvent(MBDMachine machine, @Nullable MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

}
