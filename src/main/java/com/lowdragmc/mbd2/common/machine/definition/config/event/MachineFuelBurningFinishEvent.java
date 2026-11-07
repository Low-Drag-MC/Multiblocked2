package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;

import javax.annotation.Nullable;

@Getter
//@LDLRegister(name = "MachineFuelBurningFinishEvent", group = "MachineEvent")
public class MachineFuelBurningFinishEvent extends MachineEvent implements RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    @Nullable
    public final MBDRecipe recipe;

    public MachineFuelBurningFinishEvent(MBDMachine machine, @Nullable MBDRecipe recipe) {
        this(machine, machine.getContextRecipeLogic(), recipe);
    }

    public MachineFuelBurningFinishEvent(MBDMachine machine, RecipeLogic recipeLogic, @Nullable MBDRecipe recipe) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
    }

}
