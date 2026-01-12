package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.graphprocessor.GraphParameterGet;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
@LDLRegister(name = "MachineOnRecipeFinishEvent", group = "MachineEvent")
public class MachineOnRecipeFinishEvent extends MachineEvent {
    @GraphParameterGet
    public final MBDRecipe recipe;

    public MachineOnRecipeFinishEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

}
