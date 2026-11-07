package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

@Getter
//@LDLRegister(name = "MachineOnRecipeWaitingEvent", group = "MachineEvent")
public class MachineOnRecipeWaitingEvent extends MachineEvent implements RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    public final MBDRecipe recipe;

    public MachineOnRecipeWaitingEvent(MBDMachine machine, MBDRecipe recipe) {
        this(machine, machine.getContextRecipeLogic(), recipe);
    }

    public MachineOnRecipeWaitingEvent(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
    }

}
