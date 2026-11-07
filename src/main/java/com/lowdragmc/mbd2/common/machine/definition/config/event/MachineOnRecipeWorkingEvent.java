package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
//@LDLRegister(name = "MachineOnRecipeWorkingEvent", group = "MachineEvent")
public class MachineOnRecipeWorkingEvent extends MachineEvent implements ICancellableEvent, RecipeLogicEvent {
    public final RecipeLogic recipeLogic;
    public final MBDRecipe recipe;
    public final int progress;

    public MachineOnRecipeWorkingEvent(MBDMachine machine, MBDRecipe recipe, int progress) {
        this(machine, machine.getContextRecipeLogic(), recipe, progress);
    }

    public MachineOnRecipeWorkingEvent(MBDMachine machine, RecipeLogic recipeLogic, MBDRecipe recipe, int progress) {
        super(machine);
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
        this.progress = progress;
    }

}
